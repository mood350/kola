package com.dogaa.backend.modules.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Body of {@code PUT /config/fees}: one row per KYC tier. */
public record UpdateFeesRequest(@NotEmpty @Valid List<FeeConfigResponse> fees) {
}
