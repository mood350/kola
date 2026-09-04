package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.ApiException;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.transaction.dto.BillPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.MerchantPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.service.FailedTransactionCommand;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.vault.service.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * Executes a due task through dogaa-wallet/transaction/vault (Groupe A,
 * merged into main), replacing the neutral placeholder now that those
 * modules exist.
 *
 * <p>The attempt runs in its own transaction via a {@link TransactionTemplate}
 * (REQUIRES_NEW), not an {@code @Transactional} annotation on {@link #execute}:
 * an {@code @Transactional} method that both lets an inner call fail <em>and</em>
 * catches that failure itself ends up trying to commit a transaction the inner
 * call already marked rollback-only, which throws
 * {@code UnexpectedRollbackException} instead of returning cleanly (a bug this
 * class had until a test caught it — self-invocation means splitting into two
 * {@code @Transactional} methods on the same bean doesn't work either, since
 * Spring's proxy is bypassed for calls to {@code this}). {@code TransactionTemplate}
 * sidesteps both problems: it rolls back and rethrows the original exception
 * to code that is provably outside that transaction, so {@link #recordFailure}
 * can safely start a fresh one.
 *
 * <p>This is also what merges scheduled misfires into the same transaction
 * history as everything else (DOGAA.md 4.6.3 step 4): every failure calls
 * {@link TransactionService#recordFailed}, not just {@code ScheduledTask.status}.
 */
@Service
public class LiveTaskExecutionPort implements TaskExecutionPort {

    private static final Logger log = LoggerFactory.getLogger(LiveTaskExecutionPort.class);

    private final TransactionService transactionService;
    private final VaultService vaultService;
    private final TransactionTemplate requiresNewTransaction;

    public LiveTaskExecutionPort(TransactionService transactionService, VaultService vaultService,
                                  PlatformTransactionManager transactionManager) {
        this.transactionService = transactionService;
        this.vaultService = vaultService;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean execute(ScheduledTask task) {
        try {
            requiresNewTransaction.executeWithoutResult(status -> attempt(task));
            return true;
        } catch (ApiException ex) {
            log.warn("Scheduled task {} refused: {}", task.getId(), ex.getMessage());
            recordFailure(task, ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            log.error("Scheduled task {} failed unexpectedly", task.getId(), ex);
            recordFailure(task, "Unexpected error: " + ex.getMessage());
            return false;
        }
    }

    private void attempt(ScheduledTask task) {
        switch (task.getType()) {
            case P2P_TRANSFER -> executeP2p(task);
            case MERCHANT_PAYMENT -> executeMerchantPayment(task);
            case VAULT_DEPOSIT -> executeVaultDeposit(task);
            case BILL_PAYMENT -> executeBillPayment(task);
        }
    }

    private void executeP2p(ScheduledTask task) {
        transactionService.transfer(task.getUserId(), new TransferRequest(
                task.getCurrency(), task.getAmount(), task.getBeneficiaryReference(), "Scheduled transfer"));
    }

    private void executeMerchantPayment(ScheduledTask task) {
        transactionService.payMerchant(task.getUserId(), new MerchantPaymentRequest(
                task.getCurrency(), task.getAmount(), task.getBeneficiaryReference(), "Scheduled payment"));
    }

    private void executeVaultDeposit(ScheduledTask task) {
        UUID vaultId;
        try {
            vaultId = UUID.fromString(task.getBeneficiaryReference());
        } catch (IllegalArgumentException ex) {
            // Funnelled through the same ApiException catch in execute() so it gets the
            // same FAILED trace + retry/notify handling as every other refusal.
            throw new BadRequestException("beneficiaryReference is not a valid vault id: " + task.getBeneficiaryReference());
        }
        vaultService.deposit(task.getUserId(), vaultId, task.getAmount());
    }

    private void executeBillPayment(ScheduledTask task) {
        transactionService.payBill(task.getUserId(), new BillPaymentRequest(
                task.getCurrency(), task.getAmount(), task.getBeneficiaryReference(), "Scheduled bill payment"));
    }

    private void recordFailure(ScheduledTask task, String reason) {
        TransactionType type = switch (task.getType()) {
            case P2P_TRANSFER -> TransactionType.P2P_TRANSFER;
            case MERCHANT_PAYMENT -> TransactionType.MERCHANT_PAYMENT;
            case VAULT_DEPOSIT -> TransactionType.VAULT_DEPOSIT;
            case BILL_PAYMENT -> TransactionType.BILL_PAYMENT;
        };
        transactionService.recordFailed(FailedTransactionCommand.of(
                type, task.getCurrency(), task.getAmount(), task.getUserId(),
                task.getBeneficiaryReference(), "Scheduled task " + task.getId(), reason));
    }
}
