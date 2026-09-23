package com.kola.backend.modules.notification.dto;

import com.kola.backend.common.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Sent either through the REST API (external/admin trigger) or built
 * in-process by another module's service (e.g. kola-scheduling) that injects
 * {@link com.kola.backend.modules.notification.service.NotificationService} directly.
 */
public record NotificationRequest(
        @NotNull UUID userId,
        @NotNull NotificationChannel channel,
        @NotBlank String title,
        @NotBlank String body
) {
}
