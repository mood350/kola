package com.kola.backend.modules.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Body of {@code PUT /credit/tier-config}: the whole ladder, in display order. */
public record UpdateTierConfigRequest(@NotEmpty @Valid List<TierConfigResponse> tiers) {
}
