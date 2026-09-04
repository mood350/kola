package com.dogaa.backend.modules.notification.provider;

import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.modules.notification.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder: logs instead of calling a real email gateway (e.g. SES,
 * SendGrid). Wire a real client here when one is chosen.
 */
@Component
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(Notification notification) {
        log.info("[EMAIL] to user {} - {}: {}", notification.getUserId(), notification.getTitle(), notification.getBody());
        return true;
    }
}
