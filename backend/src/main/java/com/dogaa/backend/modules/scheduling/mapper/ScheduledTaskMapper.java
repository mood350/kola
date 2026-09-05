package com.dogaa.backend.modules.scheduling.mapper;

import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;

public final class ScheduledTaskMapper {

    private ScheduledTaskMapper() {
    }

    public static ScheduledTaskResponse toResponse(ScheduledTask entity) {
        return new ScheduledTaskResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getFrequency(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getBeneficiaryReference(),
                entity.getStatus(),
                entity.getNextRunAt(),
                entity.getEndDate(),
                entity.getMaxOccurrences(),
                entity.getOccurrencesCompleted(),
                entity.getRetryCount(),
                entity.getLastFailureReason(),
                entity.getFundingVaultId(),
                entity.getBiller(),
                entity.getDayOfMonth(),
                entity.getDescription()
        );
    }
}
