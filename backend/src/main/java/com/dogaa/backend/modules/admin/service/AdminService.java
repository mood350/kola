package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.modules.admin.dto.AdminDashboardResponse;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    AdminDashboardResponse getDashboard();

    List<ScheduledTaskResponse> listAllScheduledTasks();

    List<NotificationResponse> listAllNotifications();

    CreditScoreResponse getUserScore(UUID userId);
}
