package com.dogaa.backend.modules.scoring.mapper;

import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.entity.CreditScore;

public final class CreditScoreMapper {

    private CreditScoreMapper() {
    }

    public static CreditScoreResponse toResponse(CreditScore entity) {
        return new CreditScoreResponse(
                entity.getUserId(),
                entity.getScoreValue(),
                entity.isKycEligible(),
                entity.getDepositRegularityPoints(),
                entity.getSavingsDisciplinePoints(),
                entity.getTransactionDiversityPoints(),
                entity.getBalanceStabilityPoints(),
                entity.getScheduledReliabilityPoints(),
                entity.getCreatedAt()
        );
    }
}
