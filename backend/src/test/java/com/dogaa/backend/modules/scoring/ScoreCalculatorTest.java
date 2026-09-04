package com.dogaa.backend.modules.scoring;

import com.dogaa.backend.config.ScoringProperties;
import com.dogaa.backend.modules.scoring.dto.ScoreBreakdown;
import com.dogaa.backend.modules.scoring.service.ScoreCalculator;
import com.dogaa.backend.modules.scoring.service.ScoringInputs;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreCalculatorTest {

    private final ScoringProperties properties = new ScoringProperties();
    private final ScoreCalculator calculator = new ScoreCalculator(properties);

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    /** A trader running real money through the account and saving a fifth of it. */
    private static ScoringInputs exemplaryBorrower() {
        return new ScoringInputs(
                xof("400000"), xof("260000"),
                xof("80000"), xof("80000"), BigDecimal.ZERO,
                4, 0,
                xof("120000"), xof("12000"), xof("8600"), 0,
                10, 0.15, ScoringInputs.Trend.GROWING,
                3, 7,
                3, 0, 0);
    }

    @Test
    void theWeightsAddUpToOneHundred() {
        assertThat(properties.total()).isEqualTo(100);
    }

    @Test
    void anExemplaryBorrowerScoresHigh() {
        ScoreBreakdown breakdown = calculator.calculate(exemplaryBorrower());

        assertThat(breakdown.total()).isGreaterThanOrEqualTo(85);
        assertThat(breakdown.savingsDiscipline()).isEqualTo(30);
        assertThat(breakdown.creditHistory()).isEqualTo(10);
    }

    @Test
    void anEmptyAccountScoresAlmostNothingButNotZero() {
        ScoreBreakdown breakdown = calculator.calculate(ScoringInputs.empty());

        // Only the neutral half-mark for having no repayment history yet.
        assertThat(breakdown.total()).isEqualTo(5);
        assertThat(breakdown.creditHistory()).isEqualTo(5);
        assertThat(breakdown.savingsDiscipline()).isZero();
    }

    /**
     * The attack the whole design exists to defeat: a handful of token movements, staged in one
     * evening, that used to be worth a perfect score under a presence-based model.
     *
     * <p>Amounts below the materiality threshold never reach the calculator, so what arrives here
     * is what a 500 XOF evening actually produces: no savings rate worth the name, one
     * counterparty, no history.
     */
    @Test
    void anEveningOfStagedActivityDoesNotReachTheLendingThreshold() {
        ScoringInputs staged = new ScoringInputs(
                xof("1500"), xof("1000"),
                xof("500"), xof("500"), BigDecimal.ZERO,
                0, 0,
                xof("500"), BigDecimal.ZERO, xof("33"), 0,
                1, 1.0, ScoringInputs.Trend.STABLE,
                2, 1,
                0, 0, 0);

        assertThat(calculator.calculate(staged).total()).isLessThan(60);
    }

    @Test
    void savingIsMeasuredAsAShareOfInflowsSoASmallEarnerCanScoreFullMarks() {
        ScoringInputs modest = new ScoringInputs(
                xof("20000"), xof("12000"),
                xof("3000"), xof("3000"), BigDecimal.ZERO,
                2, 0,
                xof("8000"), xof("500"), xof("400"), 0,
                8, 0.1, ScoringInputs.Trend.STABLE,
                2, 5,
                0, 0, 0);

        // 3 000 saved out of 20 000 is 15%: the full mark, on a fifth of a trader's turnover.
        assertThat(calculator.calculate(modest).savingsDiscipline()).isEqualTo(30);
    }

    @Test
    void savingThenTakingItStraightBackOutIsNotSaving() {
        ScoringInputs churner = new ScoringInputs(
                xof("400000"), xof("260000"),
                BigDecimal.ZERO, xof("80000"), xof("80000"),
                4, 0,
                xof("120000"), xof("12000"), xof("8600"), 0,
                10, 0.15, ScoringInputs.Trend.GROWING,
                3, 7,
                3, 0, 0);

        assertThat(calculator.calculate(churner).savingsDiscipline())
                .isLessThan(calculator.calculate(exemplaryBorrower()).savingsDiscipline());
    }

    @Test
    void pingPongingBetweenTwoAccountsEarnsNothingOnTheNetworkSignal() {
        ScoringInputs lonely = new ScoringInputs(
                xof("400000"), xof("260000"),
                xof("80000"), xof("80000"), BigDecimal.ZERO,
                4, 0,
                xof("120000"), xof("12000"), xof("8600"), 0,
                10, 0.15, ScoringInputs.Trend.GROWING,
                3, 1,
                3, 0, 0);

        assertThat(calculator.calculate(lonely).usageIntensity())
                .isLessThan(calculator.calculate(exemplaryBorrower()).usageIntensity());
    }

    @Test
    void anErraticBalanceScoresBelowASteadyOneOnTheSameAverage() {
        ScoringInputs steady = exemplaryBorrower();
        ScoringInputs erratic = new ScoringInputs(
                steady.totalInflow(), steady.totalOutflow(),
                steady.netSavingsIn(), steady.savingsDeposits(), steady.savingsWithdrawals(),
                steady.scheduledExecuted(), steady.scheduledFailed(),
                steady.averageBalance(), xof("110000"), steady.averageDailySpend(), 0,
                steady.distinctInflowDays(), steady.inflowIntervalCv(), steady.inflowTrend(),
                steady.distinctOutgoingTypes(), steady.distinctCounterparties(),
                steady.loansRepaidOnTime(), steady.loansPaidLate(), steady.loansDefaulted());

        assertThat(calculator.calculate(erratic).financialStability())
                .isLessThan(calculator.calculate(steady).financialStability());
    }

    @Test
    void attemptedOverdraftsCostPoints() {
        ScoringInputs base = exemplaryBorrower();
        ScoringInputs bouncing = new ScoringInputs(
                base.totalInflow(), base.totalOutflow(),
                base.netSavingsIn(), base.savingsDeposits(), base.savingsWithdrawals(),
                base.scheduledExecuted(), base.scheduledFailed(),
                base.averageBalance(), base.balanceStdDev(), base.averageDailySpend(), 4,
                base.distinctInflowDays(), base.inflowIntervalCv(), base.inflowTrend(),
                base.distinctOutgoingTypes(), base.distinctCounterparties(),
                base.loansRepaidOnTime(), base.loansPaidLate(), base.loansDefaulted());

        assertThat(calculator.calculate(bouncing).financialStability())
                .isLessThan(calculator.calculate(base).financialStability());
    }

    @Test
    void aBorrowerWithNoHistorySitsAtHalfMarksSoAFirstLoanStaysReachable() {
        ScoringInputs base = exemplaryBorrower();
        ScoringInputs newcomer = new ScoringInputs(
                base.totalInflow(), base.totalOutflow(),
                base.netSavingsIn(), base.savingsDeposits(), base.savingsWithdrawals(),
                base.scheduledExecuted(), base.scheduledFailed(),
                base.averageBalance(), base.balanceStdDev(), base.averageDailySpend(), 0,
                base.distinctInflowDays(), base.inflowIntervalCv(), base.inflowTrend(),
                base.distinctOutgoingTypes(), base.distinctCounterparties(),
                0, 0, 0);

        ScoreBreakdown breakdown = calculator.calculate(newcomer);
        assertThat(breakdown.creditHistory()).isEqualTo(5);
        assertThat(breakdown.total()).isGreaterThanOrEqualTo(60);
    }

    @Test
    void aDefaultWipesOutTheHistoryAxis() {
        ScoringInputs base = exemplaryBorrower();
        ScoringInputs defaulter = new ScoringInputs(
                base.totalInflow(), base.totalOutflow(),
                base.netSavingsIn(), base.savingsDeposits(), base.savingsWithdrawals(),
                base.scheduledExecuted(), base.scheduledFailed(),
                base.averageBalance(), base.balanceStdDev(), base.averageDailySpend(), 0,
                base.distinctInflowDays(), base.inflowIntervalCv(), base.inflowTrend(),
                base.distinctOutgoingTypes(), base.distinctCounterparties(),
                0, 0, 1);

        assertThat(calculator.calculate(defaulter).creditHistory()).isZero();
    }

    @Test
    void noAxisCanOverflowItsWeight() {
        ScoringInputs extreme = new ScoringInputs(
                xof("1000000"), xof("5000000"),
                xof("900000"), xof("900000"), BigDecimal.ZERO,
                50, 0,
                xof("9000000"), BigDecimal.ZERO, xof("1"), 0,
                30, 0.0, ScoringInputs.Trend.GROWING,
                3, 50,
                50, 0, 0);

        ScoreBreakdown breakdown = calculator.calculate(extreme);
        assertThat(breakdown.savingsDiscipline()).isLessThanOrEqualTo(30);
        assertThat(breakdown.financialStability()).isLessThanOrEqualTo(25);
        assertThat(breakdown.inflowRegularity()).isLessThanOrEqualTo(20);
        assertThat(breakdown.usageIntensity()).isLessThanOrEqualTo(15);
        assertThat(breakdown.creditHistory()).isLessThanOrEqualTo(10);
        assertThat(breakdown.total()).isLessThanOrEqualTo(100);
    }
}
