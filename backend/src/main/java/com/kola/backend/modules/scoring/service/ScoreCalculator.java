package com.kola.backend.modules.scoring.service;

import com.kola.backend.config.ScoringProperties;
import com.kola.backend.modules.scoring.dto.ScoreBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Turns behavioural measurements into a 0-100 score (KOLA.md 3.2).
 *
 * <p>Every sub-signal is a <em>ratio</em>, never a count of events. That is the whole anti-gaming
 * design: "deposited three times" costs 300 XOF to fake, while "saved 15% of what came in" cannot
 * be faked without actually having income to save from. Combined with the materiality threshold
 * applied upstream and the smoothing applied downstream, an evening of staged activity moves the
 * published score by almost nothing.
 *
 * <p>Pure arithmetic, no I/O: the rules can be exercised directly in a unit test.
 */
@Component
@RequiredArgsConstructor
public class ScoreCalculator {

    // Sub-signal shares within their axis. They sum to 1 per axis, so re-weighting an axis in
    // configuration rescales its parts instead of silently changing their balance.
    private static final double SAVINGS_RATE_SHARE = 0.50;
    private static final double SCHEDULED_SHARE = 0.34;
    private static final double PERSISTENCE_SHARE = 0.16;

    private static final double COVERAGE_SHARE = 0.48;
    private static final double SMOOTHNESS_SHARE = 0.24;
    private static final double NO_OVERDRAFT_SHARE = 0.28;

    private static final double FREQUENCY_SHARE = 0.50;
    private static final double CONSISTENCY_SHARE = 0.30;
    private static final double TREND_SHARE = 0.20;

    private static final double DIVERSITY_SHARE = 0.40;
    private static final double INTENSITY_SHARE = 0.40;
    private static final double NETWORK_SHARE = 0.20;

    /** Points lost per attempted overdraft, as a share of the sub-signal. */
    private static final double OVERDRAFT_PENALTY_SHARE = 0.30;

    private final ScoringProperties properties;

    public ScoreBreakdown calculate(ScoringInputs in) {
        // Everything behavioural is scaled by how much money actually moved. An account with no
        // real flow has no behaviour to judge: its ratios are noise, and rewarding them is what
        // let a 500 XOF evening score full marks. Repayment history is exempt — it is a fact,
        // not a ratio, and it stands on its own.
        double activity = activityFactor(in);

        int savings = scale(savingsDiscipline(in), activity, properties.getSavingsDisciplineWeight());
        int stability = scale(financialStability(in), activity, properties.getFinancialStabilityWeight());
        int inflow = scale(inflowRegularity(in), activity, properties.getInflowRegularityWeight());
        int usage = scale(usageIntensity(in), activity, properties.getUsageIntensityWeight());
        int history = creditHistory(in);

        return new ScoreBreakdown(savings, stability, inflow, usage, history,
                savings + stability + inflow + usage + history);
    }

    private double activityFactor(ScoringInputs in) {
        return saturate(ratio(in.totalInflow(), properties.getMinimumWindowInflow()));
    }

    private static int scale(int points, double factor, double axisWeight) {
        return round(points * factor, axisWeight);
    }

    /**
     * Axis A — how much of what comes in is put aside, and whether it stays aside.
     * Measured as a share of inflows, so a trader on 2M/month and a student on 20k/month can both
     * score full marks.
     */
    private int savingsDiscipline(ScoringInputs in) {
        double weight = properties.getSavingsDisciplineWeight();

        double savingsRate = ratio(in.netSavingsIn(), in.totalInflow());
        double rateTarget = properties.getSavingsRateForFullMark().doubleValue();
        double ratePoints = saturate(savingsRate / rateTarget) * weight * SAVINGS_RATE_SHARE;

        int scheduledTotal = in.scheduledExecuted() + in.scheduledFailed();
        double scheduledPoints = scheduledTotal == 0
                ? 0
                : ((double) in.scheduledExecuted() / scheduledTotal) * weight * SCHEDULED_SHARE;

        // Saving then taking it straight back out is not saving.
        double withdrawalRatio = ratio(in.savingsWithdrawals(), in.savingsDeposits());
        double persistencePoints = in.savingsDeposits().signum() == 0
                ? 0
                : saturate(1 - withdrawalRatio) * weight * PERSISTENCE_SHARE;

        return round(ratePoints + scheduledPoints + persistencePoints, weight);
    }

    /**
     * Axis B — can the account absorb a shock. Two accounts averaging 50 000 are not the same risk
     * if one is flat and the other swings between 0 and 100 000, which is why volatility counts.
     */
    private int financialStability(ScoringInputs in) {
        double weight = properties.getFinancialStabilityWeight();

        double coveragePoints;
        if (in.averageDailySpend().signum() <= 0) {
            // Nothing spent over the window: holding anything at all is the best we can tell.
            coveragePoints = in.averageBalance().signum() > 0 ? weight * COVERAGE_SHARE : 0;
        } else {
            double daysCovered = ratio(in.averageBalance(), in.averageDailySpend());
            coveragePoints = saturate(daysCovered / properties.getCoverageDaysForFullMark())
                    * weight * COVERAGE_SHARE;
        }

        double coefficientOfVariation = ratio(in.balanceStdDev(), in.averageBalance());
        double smoothnessPoints = in.averageBalance().signum() <= 0
                ? 0
                : saturate(1 - coefficientOfVariation) * weight * SMOOTHNESS_SHARE;

        double overdraftPoints = saturate(1 - OVERDRAFT_PENALTY_SHARE * in.insufficientFundsFails())
                * weight * NO_OVERDRAFT_SHARE;

        return round(coveragePoints + smoothnessPoints + overdraftPoints, weight);
    }

    /** Axis C — does money come in often, predictably, and is the activity growing. */
    private int inflowRegularity(ScoringInputs in) {
        double weight = properties.getInflowRegularityWeight();

        double frequencyPoints = saturate(
                (double) in.distinctInflowDays() / properties.getInflowDaysForFullMark())
                * weight * FREQUENCY_SHARE;

        // Three inflows spread over the month beat three on the same weekend; below three we
        // have no interval to judge.
        double consistencyPoints = in.distinctInflowDays() < 3
                ? 0
                : saturate(1 - in.inflowIntervalCv()) * weight * CONSISTENCY_SHARE;

        double trendPoints = switch (in.inflowTrend()) {
            case GROWING -> weight * TREND_SHARE;
            case STABLE -> weight * TREND_SHARE * 0.5;
            case DECLINING -> 0;
        };

        return round(frequencyPoints + consistencyPoints + trendPoints, weight);
    }

    /** Axis D — is Kola the main account or a box being ticked. The weakest signal, hence 15. */
    private int usageIntensity(ScoringInputs in) {
        double weight = properties.getUsageIntensityWeight();

        double diversityPoints = saturate(in.distinctOutgoingTypes() / 3.0) * weight * DIVERSITY_SHARE;

        double outflowRatio = ratio(in.totalOutflow(), in.totalInflow());
        double intensityPoints = saturate(
                outflowRatio / properties.getOutflowRatioForFullMark().doubleValue())
                * weight * INTENSITY_SHARE;

        // Two accounts bouncing money between themselves have one counterparty and score nothing
        // here. This is the cheapest fraud detector in the whole model.
        double networkPoints = saturate(
                (double) in.distinctCounterparties() / properties.getCounterpartiesForFullMark())
                * weight * NETWORK_SHARE;

        return round(diversityPoints + intensityPoints + networkPoints, weight);
    }

    /**
     * Axis E — repayment history, by far the best predictor once it exists.
     * A borrower with no history sits at half marks, not zero: at zero a first loan would be
     * unreachable and the product would never start.
     */
    private int creditHistory(ScoringInputs in) {
        double weight = properties.getCreditHistoryWeight();

        if (in.loansRepaidOnTime() == 0 && in.loansPaidLate() == 0 && in.loansDefaulted() == 0) {
            return round(weight * 0.5, weight);
        }
        double points = weight * 0.5
                + in.loansRepaidOnTime() * weight * 0.2
                - in.loansPaidLate() * weight * 0.5
                - in.loansDefaulted() * weight;
        return round(points, weight);
    }

    // --- helpers ---------------------------------------------------------

    /** Guards against dividing by an empty history, which is the normal state of a new account. */
    private static double ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() <= 0 || numerator == null) {
            return 0;
        }
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP).doubleValue();
    }

    /** Clamps a 0-1 fraction; anything above the target saturates rather than overflowing its axis. */
    private static double saturate(double fraction) {
        return Math.max(0, Math.min(1, fraction));
    }

    private static int round(double points, double axisWeight) {
        return (int) Math.round(Math.max(0, Math.min(axisWeight, points)));
    }
}
