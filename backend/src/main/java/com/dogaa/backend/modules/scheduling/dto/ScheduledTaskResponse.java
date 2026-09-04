package com.dogaa.backend.modules.scheduling.dto;

import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.ScheduledTaskType;

import java.math.BigDecimal;
import java.time.Instant;

public record ScheduledTaskResponse(
        Long id,
        Long userId,
        ScheduledTaskType type,
        ScheduleFrequency frequency,
        BigDecimal amount,
        String currency,
        String beneficiaryReference,
        ScheduledTaskStatus status,
        Instant nextRunAt,
        Instant endDate,
        Integer maxOccurrences,
        int occurrencesCompleted,
        int retryCount,
        String lastFailureReason
) {
}
