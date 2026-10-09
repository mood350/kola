package com.kola.backend.modules.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * One lending tier as the back-office shows and edits it (BACKEND.md 7).
 *
 * <p>{@code maxAmount} and {@code monthlyRate} travel as display strings because the screen edits
 * them as free text ("150 000 XOF", "7 %/mois"); the service parses them back into numbers and
 * rejects anything it cannot read, rather than silently storing a zero.
 *
 * @param name       tier label, ascending: "TIER 1" is the entry rung
 * @param minScore   lowest published score that reaches this tier
 * @param maxAmount  absolute ceiling for the tier
 * @param monthlyRate monthly interest
 */
public record TierConfigResponse(
        @NotBlank String name,
        @Min(0) @Max(100) int minScore,
        @NotBlank String maxAmount,
        @NotBlank String monthlyRate) {
}
