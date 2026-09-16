package com.kola.backend.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The commission one KYC tier pays (BACKEND.md 10). Rates travel as display strings ("1,50 %")
 * because the screen edits them as free text; the service parses them back.
 */
public record FeeConfigResponse(
        @NotBlank String tier,
        @NotBlank String p2p,
        @NotBlank String merchant,
        @NotBlank String cashout) {
}
