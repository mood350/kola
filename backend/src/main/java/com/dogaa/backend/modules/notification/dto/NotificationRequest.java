package com.dogaa.backend.modules.notification.dto;

import com.dogaa.backend.common.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Sent either through the REST API (external/admin trigger) or built
 * in-process by another module's service (e.g. dogaa-scheduling) that injects
 * {@link com.dogaa.backend.modules.notification.service.NotificationService} directly.
 */
public record NotificationRequest(
        @NotNull Long userId,
        @NotNull NotificationChannel channel,
        @NotBlank String title,
        @NotBlank String body
) {
}
