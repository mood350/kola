package com.kola.backend.modules.scoring.dto;

import com.kola.backend.common.enums.KycTier;

import java.time.Instant;
import java.util.UUID;

public record CreditScoreResponse(
        UUID userId,
        int scoreValue,
        int rawScoreValue,
        KycTier kycTier,
        ScoreBreakdown breakdown,
        int windowDays,
        Instant calculatedAt
) {
}
