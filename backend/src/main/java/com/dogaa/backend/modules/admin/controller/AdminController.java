package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.AdminDashboardResponse;
import com.dogaa.backend.modules.admin.service.AdminService;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/scheduling/tasks")
    public List<ScheduledTaskResponse> scheduledTasks() {
        return adminService.listAllScheduledTasks();
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications() {
        return adminService.listAllNotifications();
    }

    @GetMapping("/scoring/users/{userId}")
    public CreditScoreResponse userScore(@PathVariable Long userId) {
        return adminService.getUserScore(userId);
    }
}
