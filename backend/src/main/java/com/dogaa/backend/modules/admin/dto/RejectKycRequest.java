package com.dogaa.backend.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Why a KYC submission was turned down (BACKEND.md 6).
 *
 * <p>Mandatory: the reason is sent to the customer, and "rejected" on its own tells them nothing
 * about what to send instead — they would simply upload the same document again.
 */
public record RejectKycRequest(@NotBlank @Size(max = 300) String reason) {
}
