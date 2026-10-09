package com.kola.backend.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code PATCH /config/merchants/:id/status}: the target status the console computed. */
public record UpdateMerchantStatusRequest(@NotBlank String status) {
}
