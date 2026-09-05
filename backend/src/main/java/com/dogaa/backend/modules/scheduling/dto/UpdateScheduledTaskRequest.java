package com.dogaa.backend.modules.scheduling.dto;

import com.dogaa.backend.common.enums.Biller;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Changes to an existing schedule. Everything is optional: <b>a field left out is left alone</b>.
 *
 * <p>Two things are deliberately absent.
 *
 * <p><b>The type and the currency.</b> A standing order that turns from a transfer into a bill, or
 * from XOF into USD, is not the same instruction edited — its beneficiary, its biller and its
 * funding vault all hang off those two. Editing them in place would quietly invalidate the rest;
 * cancelling and creating anew states plainly what is happening.
 *
 * <p><b>The status.</b> Suspending and resuming are their own routes, so that "j'ai mis en pause"
 * and "j'ai changé le montant" stay separate events in the history rather than one opaque update.
 *
 * @param clearEndDate      true removes the end date — {@code endDate: null} means "unchanged",
 *                          so there has to be a way to say "no end" that is not silence
 * @param clearMaxOccurrences same, for the occurrence limit
 */
public record UpdateScheduledTaskRequest(
        @Positive BigDecimal amount,

        @Size(max = 140) String beneficiaryReference,

        Biller biller,

        /** The coffre that funds this schedule. Re-checked: owned, active, right currency. */
        UUID fundingVaultId,

        ScheduleFrequency frequency,

        @Min(1) @Max(31) Integer dayOfMonth,

        /** Moves the next occurrence. Must be in the future — the past cannot be scheduled. */
        Instant nextRunAt,

        Instant endDate,

        @Positive Integer maxOccurrences,

        boolean clearEndDate,

        boolean clearMaxOccurrences) {
}
