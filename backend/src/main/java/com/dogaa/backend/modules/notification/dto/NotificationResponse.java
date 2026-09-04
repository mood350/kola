package com.dogaa.backend.modules.notification.dto;

import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.modules.notification.entity.NotificationDeliveryStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationChannel channel,
        String title,
        String body,
        NotificationDeliveryStatus status,
        Instant sentAt
) {
}
