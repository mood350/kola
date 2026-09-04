package com.dogaa.backend.modules.scheduling.dto;

import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record ScheduledTaskRequest(
        @NotNull Long userId,
        @NotNull ScheduledTaskType type,
        @NotNull ScheduleFrequency frequency,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String beneficiaryReference,
        @NotNull Instant firstRunAt,
        Instant endDate,
        Integer maxOccurrences
) {
}
