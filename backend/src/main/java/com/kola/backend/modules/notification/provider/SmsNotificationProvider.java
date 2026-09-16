package com.kola.backend.modules.notification.provider;

import com.kola.backend.common.enums.NotificationChannel;
import com.kola.backend.modules.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder: logs instead of calling a real SMS gateway (e.g. Twilio, a
 * local mobile-money-adjacent SMS aggregator). Wire a real client here when
 * one is chosen.
 */
@Component
public class SmsNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[SMS] to user {} - {}: {}", notification.getUserId(), notification.getTitle(), notification.getBody());
        return true;
    }
}
