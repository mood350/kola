package com.dogaa.backend.modules.notification.service;

import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.modules.notification.dto.NotificationRequest;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse send(NotificationRequest request);

    List<NotificationResponse> listByUser(UUID userId);

    /**
     * Consumed by dogaa-admin (same module owner).
     */
    List<NotificationResponse> listAll();

    /**
     * Convenience used by dogaa-scheduling (same module owner) to report the
     * outcome of a scheduled task run (spec DOGAA.md §4.6.4: rappel J-1,
     * confirmation ou echec).
     */
    default void notifyTaskOutcome(UUID userId, UUID taskId, boolean success, String failureReason) {
        String title = success ? "Transaction programmee executee" : "Echec d'une transaction programmee";
        String body = success
                ? "La tache programmee #" + taskId + " a ete executee avec succes."
                : "La tache programmee #" + taskId + " a echoue : " + failureReason;
        send(new NotificationRequest(userId, NotificationChannel.PUSH, title, body));
    }
}
