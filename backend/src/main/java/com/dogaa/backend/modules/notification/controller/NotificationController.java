package com.dogaa.backend.modules.notification.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.notification.dto.NotificationRequest;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Notifications (DOGAA.md 4.6.4).
 *
 * <p>Reading is limited to one's own; sending is an administrator's or the platform's job. The
 * previous version let any signed-in user read another's notifications and send them anything,
 * which is a phishing tool rather than a feature.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Messages reçus par l'utilisateur")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @Operation(summary = "Ses propres notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> listMine(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.listByUser(currentUser.id())));
    }

    @PostMapping
    @Operation(summary = "Envoyer une notification — réservé aux administrateurs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notification envoyée", notificationService.send(request)));
    }
}
