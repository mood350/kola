package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.exception.ApiException;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.transaction.dto.BillPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.MerchantPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.vault.service.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Executes a due task through dogaa-wallet/transaction/vault (Groupe A,
 * merged into main), replacing the neutral placeholder now that those
 * modules exist. Runs in its own transaction (REQUIRES_NEW): a refusal
 * (insufficient funds, KYC limit, frozen wallet, unknown beneficiary) must
 * roll back only the attempted money movement, not the whole batch of due
 * tasks {@link ScheduledTaskRunner} is working through in its own
 * transaction — see the class comment there.
 */
@Service
public class LiveTaskExecutionPort implements TaskExecutionPort {

    private static final Logger log = LoggerFactory.getLogger(LiveTaskExecutionPort.class);

    private final TransactionService transactionService;
    private final VaultService vaultService;

    public LiveTaskExecutionPort(TransactionService transactionService, VaultService vaultService) {
        this.transactionService = transactionService;
        this.vaultService = vaultService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean execute(ScheduledTask task) {
        try {
            return switch (task.getType()) {
                case P2P_TRANSFER -> executeP2p(task);
                case MERCHANT_PAYMENT -> executeMerchantPayment(task);
                case VAULT_DEPOSIT -> executeVaultDeposit(task);
                case BILL_PAYMENT -> executeBillPayment(task);
            };
        } catch (ApiException ex) {
            log.warn("Scheduled task {} refused: {}", task.getId(), ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            log.error("Scheduled task {} failed unexpectedly", task.getId(), ex);
            return false;
        }
    }

    private boolean executeP2p(ScheduledTask task) {
        transactionService.transfer(task.getUserId(), new TransferRequest(
                task.getCurrency(), task.getAmount(), task.getBeneficiaryReference(), "Scheduled transfer"));
        return true;
    }

    private boolean executeMerchantPayment(ScheduledTask task) {
        transactionService.payMerchant(task.getUserId(), new MerchantPaymentRequest(
                task.getCurrency(), task.getAmount(), task.getBeneficiaryReference(), "Scheduled payment"));
        return true;
    }

    private boolean executeVaultDeposit(ScheduledTask task) {
        UUID vaultId;
        try {
            vaultId = UUID.fromString(task.getBeneficiaryReference());
        } catch (IllegalArgumentException ex) {
            log.warn("Scheduled task {}: beneficiaryReference is not a valid vault id", task.getId());
            return false;
        }
        vaultService.deposit(task.getUserId(), vaultId, task.getAmount());
        return true;
    }

    private boolean executeBillPayment(ScheduledTask task) {
        transactionService.payBill(task.getUserId(), new BillPaymentRequest(
                task.getCurrency(), task.getAmount(), task.getBeneficiaryReference(), "Scheduled bill payment"));
        return true;
    }
}
