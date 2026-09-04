package com.dogaa.backend.modules.scheduling;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.scheduling.service.LiveTaskExecutionPort;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.repository.TransactionRepository;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A refused scheduled execution must not vanish into ScheduledTask.status alone: it has to
 * leave the same FAILED trace a synchronous refusal would (DOGAA.md 4.6.3 step 4), so a
 * user's transaction history and a scheduler misfire are one history, not two.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduledFailureRecordingTest {

    @Autowired
    private UserService userService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private LiveTaskExecutionPort taskExecutionPort;
    @Autowired
    private TransactionRepository transactionRepository;

    private UUID userId;
    private UUID recordedFailureId;

    @BeforeEach
    void createUserWithATinyBalance() {
        User user = userService.save(User.builder()
                .firstName("Kossi").lastName("Mensah")
                .phone("+22890" + (100000 + (int) (Math.random() * 899999)))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        userId = user.getId();
        Wallet wallet = walletService.createWallet(userId, Currency.XOF);
        walletService.credit(wallet.getId(), new BigDecimal("1000"));
    }

    @AfterEach
    void cleanUp() {
        // Written through TaskExecutionPort's own REQUIRES_NEW, so it survives this test
        // method's rollback and has to be removed explicitly.
        if (recordedFailureId != null) {
            transactionRepository.deleteById(recordedFailureId);
        }
    }

    @Test
    void aRefusedScheduledTransferLeavesAFailedTransactionTrace() {
        ScheduledTask task = new ScheduledTask();
        task.setUserId(userId);
        task.setType(ScheduledTaskType.P2P_TRANSFER);
        task.setFrequency(ScheduleFrequency.ONCE);
        task.setAmount(new BigDecimal("50000")); // within the TIER_0 daily cap, above the 1000 XOF balance
        task.setCurrency(Currency.XOF);
        task.setBeneficiaryReference("+22899887766");
        task.setNextRunAt(Instant.now());

        boolean success = taskExecutionPort.execute(task);
        assertThat(success).isFalse();

        List<Transaction> failedTraces = transactionRepository.findAll().stream()
                .filter(t -> userId.equals(t.getSenderId()) && t.getStatus() == TransactionStatus.FAILED)
                .toList();
        assertThat(failedTraces).hasSize(1);

        Transaction trace = failedTraces.get(0);
        recordedFailureId = trace.getId();
        assertThat(trace.getType()).isEqualTo(TransactionType.P2P_TRANSFER);
        assertThat(trace.getCounterparty()).isEqualTo("+22899887766");
        assertThat(trace.getFailureReason()).isNotBlank();
    }
}
