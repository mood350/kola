package com.kola.backend.credit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ScoreBreakdown(
        int totalScore,
        CreditTier tier,
        BigDecimal maxLoanAmount,
        BigDecimal monthlyRate,
        LocalDateTime computedAt,
        LocalDateTime expiresAt,
        List<RuleScore> details
) {
    public record RuleScore(
            ScoringRule rule,
            String label,
            int points,
            int maxPoints,
            String explanation
    ) {}
}
