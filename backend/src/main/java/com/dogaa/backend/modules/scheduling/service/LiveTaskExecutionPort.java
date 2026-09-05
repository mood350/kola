package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.ApiException;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.transaction.dto.BillPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.MerchantPaymentRequest;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteRequest;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteResponse;
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

    /**
     * One due occurrence.
     *
     * <p>Two things happen before the payment. The vault named by the schedule is asked for the
     * full amount <em>including the commission</em>, so nothing is quietly taken from the everyday
     * balance; and the movement is stamped with a key derived from the task and the occurrence, so
     * a scheduler that restarts mid-run, or two instances that both wake at midnight, cannot pay
     * the same bill twice.
     *
     * <p>The release and the payment share this transaction on purpose: if the payment is refused,
     * the rollback puts the money back under lock rather than leaving it loose.
     */
    private void attempt(ScheduledTask task) {
        if (task.getType() != ScheduledTaskType.VAULT_DEPOSIT) {
            fundFromVault(task);
        }
        String key = idempotencyKeyFor(task);
        switch (task.getType()) {
            case P2P_TRANSFER -> executeP2p(task, key);
            case MERCHANT_PAYMENT -> executeMerchantPayment(task, key);
            case VAULT_DEPOSIT -> executeVaultDeposit(task);
            case BILL_PAYMENT -> executeBillPayment(task, key);
        }
    }

    /**
     * The same occurrence always produces the same key. The occurrence counter rather than the
     * clock, because a retry the next morning is the same instalment and must not pay again.
     */
    private String idempotencyKeyFor(ScheduledTask task) {
        return "task:" + task.getId() + ":" + task.getOccurrencesCompleted();
    }

    private void fundFromVault(ScheduledTask task) {
        if (task.getFundingVaultId() == null) {
            throw new BadRequestException(
                    "Cette planification ne désigne aucun coffre de financement");
        }
        FeeQuoteResponse quote = transactionService.quote(task.getUserId(),
                new FeeQuoteRequest(transactionTypeOf(task), task.getCurrency(), task.getAmount()));

        vaultService.releaseForPayment(task.getUserId(), task.getFundingVaultId(), quote.total());
    }

    private void executeP2p(ScheduledTask task, String key) {
        transactionService.executeIdempotent(key, () -> transactionService.transfer(
                task.getUserId(), new TransferRequest(task.getCurrency(), task.getAmount(),
                        task.getBeneficiaryReference(), "Virement programmé")));
    }

    private void executeMerchantPayment(ScheduledTask task, String key) {
        transactionService.executeIdempotent(key, () -> transactionService.payMerchant(
                task.getUserId(), new MerchantPaymentRequest(task.getCurrency(), task.getAmount(),
                        task.getBeneficiaryReference(), "Paiement programmé")));
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

    private void executeBillPayment(ScheduledTask task, String key) {
        String label = task.getBiller() == null
                ? "Facture programmée"
                : "Facture " + task.getBiller().getDisplayName();
        transactionService.executeIdempotent(key, () -> transactionService.payBill(
                task.getUserId(), new BillPaymentRequest(task.getCurrency(), task.getAmount(),
                        task.getBeneficiaryReference(), label)));
    }

    private static TransactionType transactionTypeOf(ScheduledTask task) {
        return switch (task.getType()) {
            case P2P_TRANSFER -> TransactionType.P2P_TRANSFER;
            case MERCHANT_PAYMENT -> TransactionType.MERCHANT_PAYMENT;
            case VAULT_DEPOSIT -> TransactionType.VAULT_DEPOSIT;
            case BILL_PAYMENT -> TransactionType.BILL_PAYMENT;
        };
    }

    private void recordFailure(ScheduledTask task, String reason) {
        TransactionType type = transactionTypeOf(task);
        transactionService.recordFailed(FailedTransactionCommand.of(
                type, task.getCurrency(), task.getAmount(), task.getUserId(),
                task.getBeneficiaryReference(), "Scheduled task " + task.getId(), reason));
    }
}
