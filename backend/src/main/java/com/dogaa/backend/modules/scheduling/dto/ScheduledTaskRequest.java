package com.dogaa.backend.modules.scheduling.dto;

import com.dogaa.backend.common.enums.Biller;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        Integer maxOccurrences,

        /**
         * The vault this schedule spends from. Required for everything but a vault or Bankivi
         * deposit: those two operations naturally debit the current account.
         * schedule must not help itself to the everyday balance on a date chosen weeks earlier.
         */
        UUID fundingVaultId,

        /** BILL_PAYMENT only: which biller, which decides the identifier that was asked for. */
        Biller biller,

        /**
         * MONTHLY only: the day the payment falls on, 1-31. Omitted, it is taken from
         * {@code firstRunAt}. A 29-31 lands on the last day of a shorter month and returns to the
         * chosen day afterwards.
         */
        @Min(1) @Max(31) Integer dayOfMonth
) {
}
