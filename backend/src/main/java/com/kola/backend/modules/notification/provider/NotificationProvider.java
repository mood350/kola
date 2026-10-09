package com.kola.backend.modules.notification.provider;

import com.kola.backend.common.enums.NotificationChannel;
import com.kola.backend.modules.notification.entity.Notification;

/**
 * Abstraction over a concrete delivery channel so the underlying vendor
 * (email/SMS/push gateway) can be swapped without touching NotificationService.
 */
public interface NotificationProvider {

    NotificationChannel channel();

    boolean send(Notification notification);
}
