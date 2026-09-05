package com.dogaa.backend.modules.credit.service;

import com.dogaa.backend.config.CreditProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * The leverage ladder (DOGAA.md 4.3): what a borrower may take, and at what price.
 *
 * <p>The rung is the whole risk model. At 1.0x the savings covers the principal, so a first loan
 * cannot lose money — and, just as importantly, walking away costs the borrower more than it gains
 * them. Above 1.0x that stops being true: at 1.6x, defaulting nets the borrower 60% of their own
 * savings. Higher leverage is therefore never offered on trust, only against a record of
 * repayments, and the rate falls as it rises because a proven borrower defaults far less often
 * than the extra exposure costs.
 *
 * <p>Pure arithmetic: no repository, no clock.
 */
@Component
@RequiredArgsConstructor
public class CreditPolicy {

    private final CreditProperties properties;

    /**
     * @param loansRepaid loans already paid back in full
     * @param score       the published (smoothed) score
     * @return the rung that applies, or empty when the score is below every rung
     */
    public Optional<CreditProperties.Rung> rungFor(int loansRepaid, int score) {
        return properties.getLadder().stream()
                .filter(rung -> loansRepaid >= rung.getMinLoansRepaid())
                .filter(rung -> score >= rung.getMinScore())
                .findFirst();
    }

    /**
     * Collateral times leverage, rounded down, then capped by the rung's own ceiling. Leverage
     * alone scales without limit with the borrower's savings; the ceiling is what bounds the
     * platform's exposure to a single borrower.
     */
    public BigDecimal maxLoanAmount(BigDecimal collateral, CreditProperties.Rung rung) {
        BigDecimal secured = collateral.multiply(rung.getLeverage()).setScale(0, RoundingMode.DOWN);
        BigDecimal ceiling = rung.getMaxAmount();
        return ceiling == null ? secured : secured.min(ceiling);
    }

    public BigDecimal interestOn(BigDecimal principal, BigDecimal monthlyRatePercent) {
        return principal
                .multiply(monthlyRatePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Late fees: nothing during the grace period, then a daily rate on the principal, capped.
     * A delayed salary is not a default, and an uncapped penalty turns a bad month into a debt
     * spiral — which is exactly the practice this product is meant to replace.
     */
    public BigDecimal penaltyFor(BigDecimal principal, long daysLate) {
        long chargeableDays = daysLate - properties.getGracePeriodDays();
        if (chargeableDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal penalty = principal
                .multiply(properties.getPenaltyPercentPerDay())
                .multiply(BigDecimal.valueOf(chargeableDays))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal cap = principal
                .multiply(properties.getPenaltyCapPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return penalty.min(cap);
    }
}
