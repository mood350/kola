package com.kola.backend.modules.wallet;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.modules.credit.dto.CreditEligibilityResponse;
import com.kola.backend.modules.credit.service.CreditService;
import com.kola.backend.modules.scoring.entity.CreditScore;
import com.kola.backend.modules.scoring.repository.CreditScoreRepository;
import com.kola.backend.modules.transaction.entity.Transaction;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The savings account was provisioned at sign-up and could never be funded: no route reached it.
 * Credit therefore refused every borrower for want of collateral, and the savings axis of the score
 * — 30 of its 100 points — was structurally stuck at zero.
 *
 * <p>The existing credit tests missed it because they credited the savings wallet through
 * {@code WalletService} directly, which is exactly the step a real user had no way to perform.
 * These tests go through the customer-facing path instead.
 */
@SpringBootTest
@ActiveProfiles("test")
class SavingsFundingIntegrationTest {

    @Autowired private TransactionService transactionService;
    @Autowired private WalletService walletService;
    @Autowired private CreditService creditService;
    @Autowired private UserRepository userRepository;
    @Autowired private CreditScoreRepository creditScoreRepository;

    private UUID userId;
    private Wallet current;
    private Wallet savings;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    @BeforeEach
    void setUp() {
        creditScoreRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .firstName("Kossi")
                .lastName("Adjo")
                .phone("+2289" + (int) (Math.random() * 9_000_000 + 1_000_000))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .kycTier(KycTier.TIER_2)
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        userId = user.getId();

        current = walletService.provision(userId, Currency.XOF, WalletType.CURRENT);
        savings = walletService.provision(userId, Currency.XOF, WalletType.SAVINGS);
        walletService.credit(current.getId(), xof("100000"));
    }

    // --- the movement itself ----------------------------------------------

    @Test
    void moneyMovesFromTheCurrentAccountToTheSavingsAccount() {
        transactionService.depositToSavings(userId, Currency.XOF, xof("30000"));

        assertThat(walletService.getById(current.getId()).getAvailableBalance())
                .isEqualByComparingTo("70000");
        assertThat(walletService.getById(savings.getId()).getAvailableBalance())
                .isEqualByComparingTo("30000");
    }

    /**
     * Putting money aside is not spending it. Charging a commission would tax the one behaviour
     * the product exists to encourage.
     */
    @Test
    void theMovementIsFreeAndLeavesATraceNamingBothAccounts() {
        Transaction tx = transactionService.depositToSavings(userId, Currency.XOF, xof("30000"));

        assertThat(tx.getType()).isEqualTo(TransactionType.SAVINGS_DEPOSIT);
        assertThat(tx.getFee()).isEqualByComparingTo("0");
        assertThat(tx.getSourceWalletId()).isEqualTo(current.getId());
        assertThat(tx.getDestinationWalletId()).isEqualTo(savings.getId());
    }

    /**
     * Both wallet ids on the trace are what lets the scoring collector recognise an internal move:
     * it must count as savings, and must not be counted as new income on the way in.
     */
    @Test
    void theTraceIsRecognisableAsAnInternalMove() {
        Transaction tx = transactionService.depositToSavings(userId, Currency.XOF, xof("30000"));

        assertThat(tx.getSenderId()).isEqualTo(userId);
        assertThat(tx.getRecipientId()).isEqualTo(userId);
        assertThat(tx.getSourceWalletId()).isNotNull();
        assertThat(tx.getDestinationWalletId()).isNotNull();
    }

    @Test
    void moneyComesBackOutAgain() {
        transactionService.depositToSavings(userId, Currency.XOF, xof("30000"));
        transactionService.withdrawFromSavings(userId, Currency.XOF, xof("12000"));

        assertThat(walletService.getById(savings.getId()).getAvailableBalance())
                .isEqualByComparingTo("18000");
        assertThat(walletService.getById(current.getId()).getAvailableBalance())
                .isEqualByComparingTo("82000");
    }

    @Test
    void moreThanTheCurrentBalanceCannotBeSetAside() {
        assertThatThrownBy(() ->
                transactionService.depositToSavings(userId, Currency.XOF, xof("150000")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    // --- what it unblocks -------------------------------------------------

    /**
     * The reason this matters: with no way in, the collateral was always zero and every borrower
     * was refused for want of savings, whatever their score or KYC level.
     */
    @Test
    void fundingTheSavingsAccountIsWhatMakesABorrowerEligible() {
        creditScoreRepository.save(CreditScore.builder()
                .userId(userId).scoreValue(90).rawScoreValue(90)
                .kycTier(KycTier.TIER_2).windowDays(30).build());

        CreditEligibilityResponse before = creditService.checkEligibility(userId, Currency.XOF);
        assertThat(before.eligible()).isFalse();
        assertThat(before.savingsBalance()).isEqualByComparingTo("0");

        transactionService.depositToSavings(userId, Currency.XOF, xof("50000"));

        CreditEligibilityResponse after = creditService.checkEligibility(userId, Currency.XOF);
        assertThat(after.eligible()).isTrue();
        assertThat(after.savingsBalance()).isEqualByComparingTo("50000");
        assertThat(after.maxLoanAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    /**
     * A running loan freezes the collateral into the locked balance, and a debit only ever spends
     * the available side — so the refusal is structural, not a rule of its own that a future edit
     * could forget.
     */
    @Test
    void savingsPledgedToARunningLoanCannotBeWithdrawn() {
        transactionService.depositToSavings(userId, Currency.XOF, xof("50000"));
        walletService.lock(savings.getId(), xof("50000"));

        assertThatThrownBy(() ->
                transactionService.withdrawFromSavings(userId, Currency.XOF, xof("10000")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    /** Deposits keep landing while the collateral is frozen: paying in is always allowed. */
    @Test
    void moneyCanStillBeAddedWhileTheCollateralIsFrozen() {
        transactionService.depositToSavings(userId, Currency.XOF, xof("50000"));
        walletService.lock(savings.getId(), xof("50000"));

        transactionService.depositToSavings(userId, Currency.XOF, xof("10000"));

        Wallet refreshed = walletService.getById(savings.getId());
        assertThat(refreshed.getLockedBalance()).isEqualByComparingTo("50000");
        assertThat(refreshed.getAvailableBalance()).isEqualByComparingTo("10000");
    }
}
