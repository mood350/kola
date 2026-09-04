package com.dogaa.backend.modules.notification.provider;

import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.modules.notification.entity.Notification;

/**
 * Abstraction over a concrete delivery channel so the underlying vendor
 * (email/SMS/push gateway) can be swapped without touching NotificationService.
 */
public interface NotificationProvider {

    NotificationChannel channel();

    boolean send(Notification notification);
}
