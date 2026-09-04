package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.AdminDashboardResponse;
import com.dogaa.backend.modules.admin.service.AdminService;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only back-office (DOGAA.md 4.5), scoped to the modules Groupe B owns
 * (scoring/scheduling/notification). Requires ROLE_ADMIN (enforced in
 * SecurityConfig, not here).
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Back-office: scoring/scheduling/notification metrics (ROLE_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Aggregated metrics: scheduled task and notification counts")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/scheduling/tasks")
    @Operation(summary = "List every scheduled task, across all users")
    public List<ScheduledTaskResponse> scheduledTasks() {
        return adminService.listAllScheduledTasks();
    }

    @GetMapping("/notifications")
    @Operation(summary = "List every notification, across all users")
    public List<NotificationResponse> notifications() {
        return adminService.listAllNotifications();
    }

    @GetMapping("/scoring/users/{userId}")
    @Operation(summary = "Look up a user's latest credit score")
    public CreditScoreResponse userScore(@PathVariable UUID userId) {
        return adminService.getUserScore(userId);
    }
}
