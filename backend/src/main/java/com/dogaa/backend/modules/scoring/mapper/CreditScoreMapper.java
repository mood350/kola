package com.dogaa.backend.modules.scoring.mapper;

import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.dto.ScoreBreakdown;
import com.dogaa.backend.modules.scoring.entity.CreditScore;

public final class CreditScoreMapper {

    private CreditScoreMapper() {
    }

    public static CreditScoreResponse toResponse(CreditScore entity) {
        ScoreBreakdown breakdown = new ScoreBreakdown(
                entity.getSavingsDisciplinePoints(),
                entity.getFinancialStabilityPoints(),
                entity.getInflowRegularityPoints(),
                entity.getUsageIntensityPoints(),
                entity.getCreditHistoryPoints(),
                entity.getRawScoreValue());

        return new CreditScoreResponse(
                entity.getUserId(),
                entity.getScoreValue(),
                entity.getRawScoreValue(),
                entity.getKycTier(),
                breakdown,
                entity.getWindowDays(),
                entity.getCreatedAt());
    }
}
