package com.dogaa.backend.modules.transaction;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A payment request arrives twice for reasons nobody controls: a phone that lost the response and
 * retried, a scheduler that restarted mid-run, a user who tapped twice on a slow connection.
 * Without a key the second arrival is indistinguishable from a genuine second payment.
 */
@SpringBootTest
@ActiveProfiles("test")
class IdempotencyIntegrationTest {

    @Autowired private TransactionService transactionService;
    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private UUID senderId;
    private Wallet senderWallet;
    private String recipientPhone;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .firstName("Kossi").lastName("Adjo")
                .phone("+2289" + (int) (Math.random() * 9_000_000 + 1_000_000))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .kycTier(KycTier.TIER_3)
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User sender = newUser();
        senderId = sender.getId();
        senderWallet = walletService.provision(senderId, Currency.XOF, WalletType.CURRENT);
        walletService.credit(senderWallet.getId(), xof("500000"));

        User recipient = newUser();
        recipientPhone = recipient.getPhone();
        walletService.provision(recipient.getId(), Currency.XOF, WalletType.CURRENT);
    }

    private Transaction transferWithKey(String key) {
        return transactionService.executeIdempotent(key, () ->
                transactionService.transfer(senderId,
                        new TransferRequest(Currency.XOF, xof("10000"), recipientPhone, "test")));
    }

    /** The whole point: the same key twice must move the money once. */
    @Test
    void theSameKeyTwiceMovesTheMoneyOnce() {
        BigDecimal before = walletService.getById(senderWallet.getId()).getAvailableBalance();

        Transaction first = transferWithKey("retry-me");
        Transaction second = transferWithKey("retry-me");

        assertThat(second.getReference()).isEqualTo(first.getReference());
        assertThat(second.getId()).isEqualTo(first.getId());

        BigDecimal after = walletService.getById(senderWallet.getId()).getAvailableBalance();
        // 10 000 + la commission d'un TIER_3 : 1,5 % remisés à 60 %, soit 90.
        assertThat(before.subtract(after)).isEqualByComparingTo(xof("10090"));
    }

    /**
     * The retry must look like a success, not an error. A client told "conflict" for a payment
     * that did go through has no way to tell that from one that did not.
     */
    @Test
    void theReplayReturnsTheOriginalTransactionRatherThanFailing() {
        Transaction first = transferWithKey("same-intent");
        Transaction replay = transferWithKey("same-intent");

        assertThat(replay.getStatus()).isEqualTo(first.getStatus());
        assertThat(replay.getIdempotencyKey()).isEqualTo("same-intent");
    }

    /** A new intent is a new key, and a genuine second payment must still go through. */
    @Test
    void aDifferentKeyIsADifferentPayment() {
        Transaction first = transferWithKey("intent-1");
        Transaction second = transferWithKey("intent-2");

        assertThat(second.getReference()).isNotEqualTo(first.getReference());
    }

    /** No key means no replay protection was asked for, and the movement simply runs. */
    @Test
    void anAbsentKeyStillExecutes() {
        Transaction first = transferWithKey(null);
        Transaction second = transferWithKey("   ");

        assertThat(first.getReference()).isNotEqualTo(second.getReference());
        assertThat(first.getIdempotencyKey()).isNull();
    }

    /**
     * The scheduler derives its key from the task and the occurrence counter, so a run retried the
     * next morning is the same instalment and must not pay again.
     */
    @Test
    void aScheduledOccurrenceKeyIsStableAcrossRetries() {
        UUID taskId = UUID.randomUUID();
        String key = "task:" + taskId + ":0";

        Transaction first = transferWithKey(key);
        Transaction retryNextMorning = transferWithKey(key);

        assertThat(retryNextMorning.getId()).isEqualTo(first.getId());

        // The next instalment is a different occurrence, and does pay.
        Transaction nextMonth = transferWithKey("task:" + taskId + ":1");
        assertThat(nextMonth.getId()).isNotEqualTo(first.getId());
    }
}
