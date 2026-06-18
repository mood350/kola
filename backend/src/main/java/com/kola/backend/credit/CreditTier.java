package com.kola.backend.credit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum CreditTier {

    INELIGIBLE(0, 39,   BigDecimal.ZERO,            BigDecimal.ZERO,           "Score insuffisant pour un prêt."),
    BASIC     (40, 59,  new BigDecimal("25000"),     new BigDecimal("0.030"),   "Prêt de base jusqu'à 25 000 XOF à 3%/mois."),
    STANDARD  (60, 74,  new BigDecimal("100000"),    new BigDecimal("0.020"),   "Prêt standard jusqu'à 100 000 XOF à 2%/mois."),
    PREMIUM   (75, 89,  new BigDecimal("500000"),    new BigDecimal("0.015"),   "Prêt premium jusqu'à 500 000 XOF à 1.5%/mois."),
    ELITE     (90, 100, new BigDecimal("2000000"),   new BigDecimal("0.010"),   "Prêt élite jusqu'à 2 000 000 XOF à 1%/mois.");

    private final int minScore;
    private final int maxScore;
    private final BigDecimal maxLoanAmount;
    private final BigDecimal monthlyRate;
    private final String description;

    public static CreditTier fromScore(int score) {
        for (CreditTier tier : values()) {
            if (score >= tier.minScore && score <= tier.maxScore) {
                return tier;
            }
        }
        return INELIGIBLE;
    }
}
