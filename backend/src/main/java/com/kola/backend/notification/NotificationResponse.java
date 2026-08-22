package com.kola.backend.notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String body,
        NotificationType type,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
