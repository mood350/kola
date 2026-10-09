package com.kola.backend.modules.notification.dto;

import com.kola.backend.common.enums.NotificationChannel;
import com.kola.backend.modules.notification.entity.NotificationDeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationChannel channel,
        String title,
        String body,
        NotificationDeliveryStatus status,
        Instant sentAt
) {
}
