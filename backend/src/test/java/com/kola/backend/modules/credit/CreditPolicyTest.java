package com.kola.backend.modules.credit;

import com.kola.backend.config.CreditProperties;
import com.kola.backend.modules.credit.service.CreditPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CreditPolicyTest {

    private final CreditProperties properties = new CreditProperties();
    private final CreditPolicy policy = new CreditPolicy(properties);

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    @Test
    void aFirstLoanIsCappedAtTheCollateralItself() {
        CreditProperties.Rung rung = policy.rungFor(0, 90).orElseThrow();

        assertThat(rung.getLeverage()).isEqualByComparingTo("1.0");
        assertThat(rung.getMonthlyRatePercent()).isEqualByComparingTo("8.0");
        // Leverage 1.0: the collateral covers the principal exactly, so a first loan cannot
        // lose money. Below the rung ceiling the amount is the collateral itself.
        assertThat(policy.maxLoanAmount(xof("40000"), rung)).isEqualByComparingTo("40000");
    }

    /**
     * The point of the ladder: at 1.0x the collateral covers the principal, so defaulting costs
     * the borrower more than it gains them. Leverage above 1 reverses that, which is why it is
     * only ever offered to someone who has already repaid.
     */
    @Test
    void aHighScoreAloneDoesNotBuyLeverage() {
        assertThat(policy.rungFor(0, 100).orElseThrow().getLeverage()).isEqualByComparingTo("1.0");
        assertThat(policy.rungFor(3, 100).orElseThrow().getLeverage()).isEqualByComparingTo("1.6");
    }

    @Test
    void repaymentsAndScoreClimbTheLadderTogether() {
        assertThat(policy.rungFor(1, 60).orElseThrow().getLeverage()).isEqualByComparingTo("1.2");
        assertThat(policy.rungFor(2, 75).orElseThrow().getLeverage()).isEqualByComparingTo("1.4");
        assertThat(policy.rungFor(3, 85).orElseThrow().getLeverage()).isEqualByComparingTo("1.6");

        // Three repayments but a score that slipped: back down a rung.
        assertThat(policy.rungFor(3, 78).orElseThrow().getLeverage()).isEqualByComparingTo("1.4");
    }

    @Test
    void theRateFallsAsTheLeverageRises() {
        assertThat(policy.rungFor(0, 60).orElseThrow().getMonthlyRatePercent())
                .isEqualByComparingTo("8.0");
        assertThat(policy.rungFor(3, 85).orElseThrow().getMonthlyRatePercent())
                .isEqualByComparingTo("6.0");
    }

    @Test
    void aScoreBelowTheFloorReachesNoRungAtAll() {
        assertThat(policy.rungFor(5, 59)).isEmpty();
    }

    @Test
    void theTopRungLendsSixtyPercentMoreThanTheSavings() {
        CreditProperties.Rung rung = policy.rungFor(3, 90).orElseThrow();
        BigDecimal amount = policy.maxLoanAmount(xof("500000"), uncapped(rung));

        assertThat(amount).isEqualByComparingTo("800000");
        assertThat(amount.add(policy.interestOn(amount, rung.getMonthlyRatePercent())))
                .isEqualByComparingTo("848000");
    }

    @Test
    void interestFollowsTheSpecExample() {
        // KOLA.md 5.3.B: 100 000 borrowed at 8% a month is repaid as 108 000.
        assertThat(policy.interestOn(xof("100000"), xof("8.0"))).isEqualByComparingTo("8000");
    }

    @Test
    void thereIsNoPenaltyDuringTheGracePeriod() {
        assertThat(policy.penaltyFor(xof("500000"), 1)).isEqualByComparingTo("0");
        assertThat(policy.penaltyFor(xof("500000"), 3)).isEqualByComparingTo("0");
    }

    @Test
    void thePenaltyStartsAfterTheGracePeriod() {
        // Day 5 is two chargeable days at 0.5% of 500 000.
        assertThat(policy.penaltyFor(xof("500000"), 5)).isEqualByComparingTo("5000");
    }

    @Test
    void thePenaltyIsCappedSoALateMonthDoesNotBecomeADebtSpiral() {
        assertThat(policy.penaltyFor(xof("500000"), 365)).isEqualByComparingTo("75000");
    }

    /**
     * The rung ceiling bounds the platform's exposure to one borrower, whatever the collateral.
     * It binds before leverage does: 500 000 of savings at 1.6x would reach 800 000, but the top
     * rung stops at 750 000.
     */
    @Test
    void theRungCeilingCapsWhatLeverageWouldOtherwiseAllow() {
        CreditProperties.Rung rung = properties.getLadder().get(0);

        assertThat(rung.getMaxAmount()).isEqualByComparingTo("750000");
        assertThat(policy.maxLoanAmount(xof("500000"), rung)).isEqualByComparingTo("750000");
    }

    /** A null ceiling means unlimited, not zero — the same convention as the KYC limits. */
    @Test
    void aRungWithoutACeilingIsBoundedOnlyByLeverage() {
        CreditProperties.Rung rung = properties.getLadder().get(0);

        assertThat(policy.maxLoanAmount(xof("500000"), uncapped(rung)))
                .isEqualByComparingTo("800000");
    }

    /** The same rung with its ceiling removed, to isolate the leverage arithmetic. */
    private static CreditProperties.Rung uncapped(CreditProperties.Rung rung) {
        CreditProperties.Rung copy = new CreditProperties.Rung();
        copy.setMinLoansRepaid(rung.getMinLoansRepaid());
        copy.setMinScore(rung.getMinScore());
        copy.setLeverage(rung.getLeverage());
        copy.setMonthlyRatePercent(rung.getMonthlyRatePercent());
        copy.setMaxAmount(null);
        return copy;
    }
}
