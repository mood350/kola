package com.dogaa.backend.modules.transaction;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.repository.TransactionRepository;
import com.dogaa.backend.modules.transaction.service.FailedTransactionCommand;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code recordFailed} is what the scheduler calls when a due item cannot run. It must
 * persist a standalone {@code FAILED} trace (its own transaction, so a rolled-back execution
 * attempt does not take the trace with it).
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;

    private UUID createdId;

    @AfterEach
    void cleanUp() {
        if (createdId != null) {
            transactionRepository.deleteById(createdId);
        }
    }

    @Test
    void recordFailedPersistsAFailedTraceWithTheReason() {
        UUID senderId = UUID.randomUUID();
        Transaction failed = transactionService.recordFailed(FailedTransactionCommand.of(
                TransactionType.P2P_TRANSFER, Currency.XOF, new BigDecimal("25000"),
                senderId, "+22890111222", "Scheduled rent", "Insufficient funds"));
        createdId = failed.getId();

        assertThat(failed.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("Insufficient funds");
        assertThat(failed.getReference()).startsWith("TXN-");

        Transaction reloaded = transactionRepository.findById(createdId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(reloaded.getSenderId()).isEqualTo(senderId);
        assertThat(reloaded.getFee()).isEqualByComparingTo("0");
        assertThat(reloaded.getCompletedAt()).isNull();
    }
}
