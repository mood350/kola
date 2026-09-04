package com.dogaa.backend.modules.credit;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.InsufficientFundsException;
import com.dogaa.backend.modules.credit.dto.CreditEligibilityResponse;
import com.dogaa.backend.modules.credit.dto.LoanRequest;
import com.dogaa.backend.modules.credit.dto.LoanResponse;
import com.dogaa.backend.modules.credit.entity.Loan;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.credit.service.CreditService;
import com.dogaa.backend.modules.scoring.entity.CreditScore;
import com.dogaa.backend.modules.scoring.repository.CreditScoreRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CreditFlowIntegrationTest {

    @Autowired
    private CreditService creditService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private LoanRepository loanRepository;

    private UUID userId;
    private Wallet current;
    private Wallet savings;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
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
    }

    /** Puts a score on file directly: the scoring engine has its own tests. */
    private void givenScore(int score) {
        creditScoreRepository.save(CreditScore.builder()
                .userId(userId)
                .scoreValue(score)
                .rawScoreValue(score)
                .kycTier(KycTier.TIER_2)
                .windowDays(30)
                .build());
    }

    private void givenSavings(String amount) {
        walletService.credit(savings.getId(), xof(amount));
    }

    // --- eligibility ------------------------------------------------------

    @Test
    void theCeilingIsTheSavingsBalanceForAFirstLoan() {
        givenScore(90);
        givenSavings("500000");

        CreditEligibilityResponse eligibility = creditService.checkEligibility(userId, Currency.XOF);

        assertThat(eligibility.eligible()).isTrue();
        assertThat(eligibility.savingsBalance()).isEqualByComparingTo("500000");
        // A perfect score buys no leverage on a first loan: the collateral must cover the principal.
        assertThat(eligibility.leverageRatio()).isEqualByComparingTo("1.0");
        assertThat(eligibility.maxLoanAmount()).isEqualByComparingTo("500000");
        assertThat(eligibility.monthlyRatePercent()).isEqualByComparingTo("8.0");
        assertThat(eligibility.totalRepayable()).isEqualByComparingTo("540000");
    }

    @Test
    void anEmptySavingsAccountBlocksTheLoanAndSaysWhy() {
        givenScore(90);

        CreditEligibilityResponse eligibility = creditService.checkEligibility(userId, Currency.XOF);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.maxLoanAmount()).isEqualByComparingTo("0");
        assertThat(eligibility.blockers()).anyMatch(b -> b.contains("savings account must hold"));
    }

    @Test
    void tier1IsRefusedEvenWithSavingsAndAPerfectScore() {
        User user = userRepository.findById(userId).orElseThrow();
        user.setKycTier(KycTier.TIER_1);
        userRepository.save(user);
        givenScore(100);
        givenSavings("500000");

        CreditEligibilityResponse eligibility = creditService.checkEligibility(userId, Currency.XOF);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.blockers()).anyMatch(b -> b.contains("TIER_2"));
    }

    @Test
    void aScoreBelowSixtyIsRefusedAndTheGapIsSpelledOut() {
        givenScore(45);
        givenSavings("500000");

        CreditEligibilityResponse eligibility = creditService.checkEligibility(userId, Currency.XOF);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.blockers()).anyMatch(b -> b.contains("45/100"));
    }

    // --- the loan itself --------------------------------------------------

    @Test
    void borrowingPaysTheCurrentAccountAndFreezesTheSavings() {
        givenScore(90);
        givenSavings("500000");

        LoanResponse loan = creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("500000")));

        assertThat(loan.principal()).isEqualByComparingTo("500000");
        assertThat(loan.interestAmount()).isEqualByComparingTo("40000");
        assertThat(loan.totalDue()).isEqualByComparingTo("540000");
        assertThat(loan.status()).isEqualTo(LoanStatus.ACTIVE);

        assertThat(walletService.getById(current.getId()).getAvailableBalance())
                .isEqualByComparingTo("500000");

        Wallet frozen = walletService.getById(savings.getId());
        assertThat(frozen.getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(frozen.getLockedBalance()).isEqualByComparingTo("500000");
    }

    /** The two halves of the rule the product is built on: no taking out, yes putting in. */
    @Test
    void whileALoanRunsTheSavingsCannotBeWithdrawnButCanStillBeToppedUp() {
        givenScore(90);
        givenSavings("500000");
        creditService.requestLoan(userId, new LoanRequest(Currency.XOF, xof("500000")));

        assertThatThrownBy(() -> walletService.debit(savings.getId(), xof("1000")))
                .isInstanceOf(InsufficientFundsException.class);

        walletService.credit(savings.getId(), xof("50000"));
        assertThat(walletService.getById(savings.getId()).getTotalBalance())
                .isEqualByComparingTo("550000");
    }

    @Test
    void borrowingMoreThanTheCeilingIsRefused() {
        givenScore(90);
        givenSavings("500000");

        assertThatThrownBy(() -> creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("800000"))))
                .hasMessageContaining("The most you can borrow is 500000");
    }

    @Test
    void onlyOneLoanRunsAtATime() {
        givenScore(90);
        givenSavings("500000");
        creditService.requestLoan(userId, new LoanRequest(Currency.XOF, xof("100000")));

        assertThatThrownBy(() -> creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("100000"))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have a loan running");
    }

    // --- repayment --------------------------------------------------------

    @Test
    void repayingInFullReleasesTheCollateral() {
        givenScore(90);
        givenSavings("500000");
        LoanResponse loan = creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("500000")));

        // The interest has to come from somewhere other than the loan itself.
        walletService.credit(current.getId(), xof("40000"));

        LoanResponse repaid = creditService.repay(userId, loan.id(), null);

        assertThat(repaid.status()).isEqualTo(LoanStatus.REPAID);
        assertThat(repaid.outstanding()).isEqualByComparingTo("0");

        Wallet released = walletService.getById(savings.getId());
        assertThat(released.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(released.getAvailableBalance()).isEqualByComparingTo("500000");
    }

    @Test
    void aPartialRepaymentLeavesTheCollateralFrozen() {
        givenScore(90);
        givenSavings("500000");
        LoanResponse loan = creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("500000")));

        LoanResponse partial = creditService.repay(userId, loan.id(), xof("200000"));

        assertThat(partial.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(partial.outstanding()).isEqualByComparingTo("340000");
        assertThat(walletService.getById(savings.getId()).getLockedBalance())
                .isEqualByComparingTo("500000");
    }

    // --- the ladder -------------------------------------------------------

    @Test
    void aRepaidLoanUnlocksTheNextRungOfLeverage() {
        givenScore(90);
        givenSavings("500000");
        LoanResponse first = creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("500000")));
        walletService.credit(current.getId(), xof("40000"));
        creditService.repay(userId, first.id(), null);

        CreditEligibilityResponse next = creditService.checkEligibility(userId, Currency.XOF);

        assertThat(next.loansRepaid()).isEqualTo(1);
        assertThat(next.leverageRatio()).isEqualByComparingTo("1.2");
        assertThat(next.maxLoanAmount()).isEqualByComparingTo("600000");
        // The rate falls as the leverage rises: a proven borrower is cheaper to lend to.
        assertThat(next.monthlyRatePercent()).isEqualByComparingTo("7.0");
    }

    // --- recovery ---------------------------------------------------------

    @Test
    void recoveryTakesTheCurrentAccountFirstThenSeizesTheCollateral() {
        givenScore(90);
        givenSavings("500000");
        LoanResponse loan = creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("500000")));

        // The borrower spent the money and let the loan run past its window.
        walletService.debit(current.getId(), xof("500000"));
        Loan overdue = loanRepository.findById(loan.id()).orElseThrow();
        overdue.setDueAt(Instant.now().minus(45, ChronoUnit.DAYS));
        loanRepository.save(overdue);

        LoanResponse recovered = creditService.recover(loan.id());

        assertThat(recovered.status()).isEqualTo(LoanStatus.DEFAULTED);
        assertThat(walletService.getById(savings.getId()).getTotalBalance())
                .isEqualByComparingTo("0");

        // At 1.0x leverage the collateral covers the principal, so the shortfall is only the
        // interest and the late penalty. This is why a first loan cannot lose the lender money.
        assertThat(recovered.shortfallAmount()).isLessThan(xof("120000"));
        assertThat(recovered.shortfallAmount()).isPositive();
    }

    @Test
    void aDefaultKeepsTheBorrowerOffTheLadder() {
        givenScore(90);
        givenSavings("500000");
        LoanResponse loan = creditService.requestLoan(userId,
                new LoanRequest(Currency.XOF, xof("500000")));
        walletService.debit(current.getId(), xof("500000"));
        Loan overdue = loanRepository.findById(loan.id()).orElseThrow();
        overdue.setDueAt(Instant.now().minus(45, ChronoUnit.DAYS));
        loanRepository.save(overdue);
        creditService.recover(loan.id());

        CreditEligibilityResponse after = creditService.checkEligibility(userId, Currency.XOF);

        assertThat(after.loansRepaid()).isZero();
        assertThat(after.eligible()).isFalse();
    }
}
