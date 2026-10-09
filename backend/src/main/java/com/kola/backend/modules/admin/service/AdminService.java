package com.kola.backend.modules.admin.service;

import com.kola.backend.modules.admin.dto.AdminDashboardResponse;
import com.kola.backend.modules.notification.dto.NotificationResponse;
import com.kola.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.kola.backend.modules.scoring.dto.CreditScoreResponse;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    AdminDashboardResponse getDashboard();

    List<ScheduledTaskResponse> listAllScheduledTasks();

    List<NotificationResponse> listAllNotifications();

    CreditScoreResponse getUserScore(UUID userId);
}
