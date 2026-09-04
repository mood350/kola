package com.dogaa.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Weights and thresholds of the alternative credit score (DOGAA.md 3.2).
 *
 * <p>They live in configuration because the right weighting is an empirical question: nobody knows
 * on day one which signal predicts repayment, and the answer changes once real repayment data
 * exists. The five weights must add up to 100.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.scoring")
public class ScoringProperties {

    /** Rolling observation window, in days. */
    private int windowDays = 30;

    private int savingsDisciplineWeight = 30;
    private int financialStabilityWeight = 25;
    private int inflowRegularityWeight = 20;
    private int usageIntensityWeight = 15;
    private int creditHistoryWeight = 10;

    /**
     * Transactions below this amount are invisible to the scoring. They cost nothing to fabricate,
     * so they must earn nothing — this single threshold is what stops a 500 XOF evening from
     * producing a 100/100 score.
     */
    private BigDecimal materialityThreshold = new BigDecimal("500");

    /**
     * Inflow over the window below which the behavioural axes are damped proportionally.
     *
     * <p>Ratios alone are gameable at any scale: saving 500 out of 1 500 is the same 33% as saving
     * 300 000 out of 900 000, and the first costs an evening. Scaling the four behavioural axes by
     * how much money actually moved keeps the ratios fair to small earners while making a staged
     * account worth almost nothing.
     */
    private BigDecimal minimumWindowInflow = new BigDecimal("20000");

    /** Share of inflows saved that earns the full savings mark. */
    private BigDecimal savingsRateForFullMark = new BigDecimal("0.15");

    /** Distinct days with an inflow that earn the full frequency mark. */
    private int inflowDaysForFullMark = 8;

    /** Days of spending the average balance must cover to earn the full coverage mark. */
    private int coverageDaysForFullMark = 7;

    /** Distinct counterparties that earn the full network mark: the anti ping-pong signal. */
    private int counterpartiesForFullMark = 5;

    /** Ratio of outgoing to incoming money that earns the full intensity mark. */
    private BigDecimal outflowRatioForFullMark = new BigDecimal("0.6");

    /**
     * Weight of today's raw score in the published one; the rest carries over from yesterday.
     * An exponential moving average means a single good evening moves nothing: converging takes
     * weeks of consistent behaviour, which is precisely the behaviour worth rewarding.
     */
    private BigDecimal smoothingFactor = new BigDecimal("0.7");

    public int total() {
        return savingsDisciplineWeight + financialStabilityWeight + inflowRegularityWeight
                + usageIntensityWeight + creditHistoryWeight;
    }
}
