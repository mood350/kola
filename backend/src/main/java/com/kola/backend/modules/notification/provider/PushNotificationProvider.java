package com.kola.backend.modules.notification.provider;

import com.kola.backend.common.enums.NotificationChannel;
import com.kola.backend.modules.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder: logs instead of calling a real push gateway (e.g. FCM/APNs).
 * Wire a real client here when one is chosen.
 */
@Component
public class PushNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[PUSH] to user {} - {}: {}", notification.getUserId(), notification.getTitle(), notification.getBody());
        return true;
    }
}
