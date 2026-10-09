package com.kola.backend.modules.transaction;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.config.KycProperties;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.modules.transaction.dto.TransferRequest;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.repository.UserRepository;
import com.kola.backend.modules.wallet.entity.Wallet;
import com.kola.backend.modules.wallet.service.WalletService;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The ceilings that are enforced must be the ceilings that are configured.
 *
 * <p>There used to be two sets: {@code app.kyc.limits.*}, which the KYC screen displayed, and a
 * hard-coded table inside {@code KycLimitPolicy}, which was what actually ran. They had already
 * drifted — TIER_1 was shown 300 000 while 500 000 went through — and the per-transaction, monthly
 * and balance ceilings were configured, displayed, and enforced nowhere.
 *
 * <p>These tests read the limits from {@link KycProperties} rather than restating them, so they
 * check the wiring rather than a copy of the numbers: change a property and they follow.
 */
@SpringBootTest
@ActiveProfiles("test")
class KycLimitEnforcementIntegrationTest {

    @Autowired private TransactionService transactionService;
    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;
    @Autowired private KycProperties kycProperties;

    private UUID senderId;
    private Wallet senderWallet;
    private String recipientPhone;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    private static String randomPhone() {
        return "+2289" + (int) (Math.random() * 9_000_000 + 1_000_000);
    }

    private User newUser(KycTier tier) {
        return userRepository.save(User.builder()
                .firstName("Kossi").lastName("Adjo")
                .phone(randomPhone())
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .kycTier(tier)
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User sender = newUser(KycTier.TIER_0);
        senderId = sender.getId();
        senderWallet = walletService.provision(senderId, Currency.XOF, WalletType.CURRENT);
        walletService.provision(senderId, Currency.XOF, WalletType.SAVINGS);
        walletService.credit(senderWallet.getId(), xof("100000000"));

        User recipient = newUser(KycTier.TIER_3);
        recipientPhone = recipient.getPhone();
        walletService.provision(recipient.getId(), Currency.XOF, WalletType.CURRENT);
    }

    private void send(BigDecimal amount) {
        transactionService.transfer(senderId,
                new TransferRequest(Currency.XOF, amount, recipientPhone, "test"));
    }

    private KycProperties.TierLimits tier0() {
        return kycProperties.limitsFor(KycTier.TIER_0);
    }

    // --- the ceilings that were enforced nowhere --------------------------

    /** A single operation above the per-transaction ceiling was going through unchecked. */
    @Test
    void theSingleOperationCeilingIsEnforced() {
        BigDecimal justOver = tier0().getPerTransaction().add(BigDecimal.ONE);

        assertThatThrownBy(() -> send(justOver))
                .isInstanceOf(KycLimitExceededException.class);
    }

    @Test
    void anOperationAtTheCeilingIsAllowed() {
        assertThatCode(() -> send(tier0().getPerTransaction())).doesNotThrowAnyException();
    }

    /** The daily ceiling is cumulative: the second transfer is what crosses it. */
    @Test
    void theDailyCeilingCountsWhatHasAlreadyBeenSent() {
        BigDecimal perTransaction = tier0().getPerTransaction();
        BigDecimal daily = tier0().getDaily();

        long allowedBeforeCrossing = daily.divideToIntegralValue(perTransaction).longValue();
        for (long i = 0; i < allowedBeforeCrossing; i++) {
            send(perTransaction);
        }

        assertThatThrownBy(() -> send(perTransaction))
                .isInstanceOf(KycLimitExceededException.class);
    }

    // --- the configured values are the ones that bite ---------------------

    /**
     * The regression that mattered: the enforced daily ceiling used to be 500 000 for TIER_1 while
     * the configuration — and the screen — said 300 000. Sending just over the configured value
     * must fail, whatever a table elsewhere might once have said.
     */
    @Test
    void theEnforcedDailyCeilingIsTheConfiguredOneNotAHardCodedTable() {
        User tier1 = newUser(KycTier.TIER_1);
        Wallet wallet = walletService.provision(tier1.getId(), Currency.XOF, WalletType.CURRENT);
        walletService.credit(wallet.getId(), xof("100000000"));

        KycProperties.TierLimits limits = kycProperties.limitsFor(KycTier.TIER_1);
        BigDecimal perTransaction = limits.getPerTransaction();

        long allowed = limits.getDaily().divideToIntegralValue(perTransaction).longValue();
        for (long i = 0; i < allowed; i++) {
            transactionService.transfer(tier1.getId(),
                    new TransferRequest(Currency.XOF, perTransaction, recipientPhone, "test"));
        }

        assertThatThrownBy(() -> transactionService.transfer(tier1.getId(),
                new TransferRequest(Currency.XOF, perTransaction, recipientPhone, "test")))
                .isInstanceOf(KycLimitExceededException.class);
    }

    /** TIER_3 has null ceilings, and null means unlimited — never zero. */
    @Test
    void theTopTierIsUncapped() {
        User tier3 = newUser(KycTier.TIER_3);
        Wallet wallet = walletService.provision(tier3.getId(), Currency.XOF, WalletType.CURRENT);
        walletService.credit(wallet.getId(), xof("100000000"));

        assertThat(kycProperties.limitsFor(KycTier.TIER_3).getDaily()).isNull();
        assertThatCode(() -> transactionService.transfer(tier3.getId(),
                new TransferRequest(Currency.XOF, xof("20000000"), recipientPhone, "test")))
                .doesNotThrowAnyException();
    }

    // --- the balance ceiling ----------------------------------------------

    /**
     * Checked on cash-in, where the answer — raise your KYC level — is the user's own to act on.
     * It is deliberately not checked on an incoming transfer: bouncing a payment because the
     * recipient is near their ceiling punishes the sender for someone else's paperwork.
     */
    @Test
    void theBalanceCeilingIsEnforcedOnCashIn() {
        User fresh = newUser(KycTier.TIER_0);
        walletService.provision(fresh.getId(), Currency.XOF, WalletType.CURRENT);

        BigDecimal cap = kycProperties.limitsFor(KycTier.TIER_0).getBalanceCap();

        assertThatCode(() -> transactionService.cashIn(fresh.getId(), Currency.XOF, cap))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> transactionService.cashIn(fresh.getId(), Currency.XOF, BigDecimal.ONE))
                .isInstanceOf(KycLimitExceededException.class);
    }

    /** Moving money between one's own accounts is not spending, and must not consume an allowance. */
    @Test
    void aSavingsMovementDoesNotEatIntoTheSendAllowance() {
        BigDecimal daily = tier0().getDaily();

        transactionService.depositToSavings(senderId, Currency.XOF, daily.multiply(xof("10")));

        assertThatCode(() -> send(tier0().getPerTransaction())).doesNotThrowAnyException();
    }
}
