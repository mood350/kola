package com.dogaa.backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Loan terms (DOGAA.md 4.3), secured against the savings account.
 *
 * <p>The leverage ladder is the heart of the risk model. At 1.0x the collateral covers the whole
 * principal, so a first loan cannot lose money and defaulting costs the borrower more than it
 * gains them. Above 1.0x that stops being true — walking away becomes profitable — so higher
 * leverage is only ever offered to borrowers who have already repaid.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.credit")
public class CreditProperties {

    /** Below this score no credit line opens, whatever the collateral. */
    private int minimumScore = 60;

    private int termDays = 30;

    /** No penalty for the first days late: a delayed salary is not a default. */
    private int gracePeriodDays = 3;

    private BigDecimal penaltyPercentPerDay = new BigDecimal("0.5");

    private BigDecimal penaltyCapPercent = new BigDecimal("15");

    /** Days past due after which recovery runs and the collateral is seized. */
    private int defaultAfterDays = 30;

    /** Smallest savings balance that can back a loan. */
    private BigDecimal minimumCollateral = new BigDecimal("5000");

    /** Minimum account age. Set to 0 for demos, where every account is created the same day. */
    private int minimumAccountAgeDays = 0;

    /**
     * Read top-down; the first rung whose two conditions are met wins. {@code loansRepaid} is how
     * many loans the borrower has already paid back in full.
     */
    private List<Rung> ladder = List.of(
            new Rung(3, 85, new BigDecimal("1.6"), new BigDecimal("6.0"), new BigDecimal("750000")),
            new Rung(2, 75, new BigDecimal("1.4"), new BigDecimal("6.5"), new BigDecimal("300000")),
            new Rung(1, 60, new BigDecimal("1.2"), new BigDecimal("7.0"), new BigDecimal("150000")),
            new Rung(0, 60, new BigDecimal("1.0"), new BigDecimal("8.0"), new BigDecimal("50000")));

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rung {
        private int minLoansRepaid;
        private int minScore;
        /** Multiple of the savings balance that may be borrowed. */
        private BigDecimal leverage;
        /** Monthly interest, in percent. */
        private BigDecimal monthlyRatePercent;
        /**
         * Absolute ceiling for the rung, whatever the collateral (DOGAA.md 4.3, "limites
         * différentes" per tier). Leverage alone would let a large saver borrow without bound;
         * this caps the platform's exposure to any single borrower. Null means no ceiling.
         */
        private BigDecimal maxAmount;
    }
}
