package com.dogaa.backend.modules.notification.controller;

import com.dogaa.backend.modules.notification.dto.NotificationRequest;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Email/SMS/push dispatch")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a notification on a given channel")
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        return notificationService.send(request);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "List a user's notification history")
    public List<NotificationResponse> listByUser(@PathVariable UUID userId) {
        return notificationService.listByUser(userId);
    }
}
