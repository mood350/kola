package com.dogaa.backend.modules.credit.dto;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.modules.scoring.dto.ScoreBreakdown;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the loan screen needs in one call.
 *
 * <p>When {@code eligible} is false, {@code blockers} says exactly what is missing. A refusal a
 * user cannot act on is a user lost.
 *
 * @param maxLoanAmount    collateral times leverage, zero when not eligible
 * @param totalRepayable   what would have to be paid back at the due date
 */
public record CreditEligibilityResponse(
        boolean eligible,
        int score,
        int minimumScore,
        KycTier kycTier,
        ScoreBreakdown breakdown,
        Currency currency,
        BigDecimal savingsBalance,
        BigDecimal leverageRatio,
        BigDecimal maxLoanAmount,
        BigDecimal monthlyRatePercent,
        BigDecimal totalRepayable,
        int termDays,
        int loansRepaid,
        List<String> blockers) {
}
