package com.dogaa.backend.modules.scoring.dto;

import java.time.Instant;
import java.util.UUID;

public record CreditScoreResponse(
        UUID userId,
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
