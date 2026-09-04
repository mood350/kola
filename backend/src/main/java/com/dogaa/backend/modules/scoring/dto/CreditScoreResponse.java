package com.dogaa.backend.modules.scoring.dto;

import java.time.Instant;

public record CreditScoreResponse(
        Long userId,
        int scoreValue,
        boolean kycEligible,
        int depositRegularityPoints,
        int savingsDisciplinePoints,
        int transactionDiversityPoints,
        int balanceStabilityPoints,
        int scheduledReliabilityPoints,
        Instant calculatedAt
) {
}
