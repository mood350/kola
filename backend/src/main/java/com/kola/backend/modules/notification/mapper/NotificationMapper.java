package com.kola.backend.modules.notification.mapper;

import com.kola.backend.modules.notification.dto.NotificationResponse;
import com.kola.backend.modules.notification.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getChannel(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                entity.getSentAt()
        );
    }
}
