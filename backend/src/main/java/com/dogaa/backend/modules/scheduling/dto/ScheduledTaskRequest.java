package com.dogaa.backend.modules.scheduling.dto;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A task to schedule.
 *
 * <p>There is no {@code userId} field on purpose: the owner comes from the token. Keeping one that
 * the server ignores would look authoritative and mislead the next caller. A payload that still
 * carries it is accepted — the field is simply not read.
 */
public record ScheduledTaskRequest(
        @NotNull ScheduledTaskType type,
        @NotNull ScheduleFrequency frequency,
        @NotNull @Positive BigDecimal amount,
        @NotNull Currency currency,
        @NotBlank String beneficiaryReference,
        @NotNull Instant firstRunAt,
        Instant endDate,
        Integer maxOccurrences
) {
}
