package com.dogaa.backend.modules.scoring.dto;

import com.dogaa.backend.common.enums.KycTier;

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
