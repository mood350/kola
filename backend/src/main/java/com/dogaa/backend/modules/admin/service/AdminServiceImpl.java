package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.modules.admin.dto.AdminDashboardResponse;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.notification.entity.NotificationDeliveryStatus;
import com.dogaa.backend.modules.notification.service.NotificationService;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.service.ScoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final ScheduledTaskService scheduledTaskService;
    private final NotificationService notificationService;
    private final ScoringService scoringService;

    public AdminServiceImpl(ScheduledTaskService scheduledTaskService,
                             NotificationService notificationService,
                             ScoringService scoringService) {
        this.scheduledTaskService = scheduledTaskService;
        this.notificationService = notificationService;
        this.scoringService = scoringService;
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        List<ScheduledTaskResponse> tasks = scheduledTaskService.listAll();
        List<NotificationResponse> notifications = notificationService.listAll();

        long active = tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.ACTIVE).count();
        long failed = tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.FAILED).count();
        long sent = notifications.stream().filter(n -> n.status() == NotificationDeliveryStatus.SENT).count();
        long notificationsFailed = notifications.stream().filter(n -> n.status() == NotificationDeliveryStatus.FAILED).count();

        return new AdminDashboardResponse(tasks.size(), active, failed, sent, notificationsFailed);
    }

    @Override
    public List<ScheduledTaskResponse> listAllScheduledTasks() {
        return scheduledTaskService.listAll();
    }

    @Override
    public List<NotificationResponse> listAllNotifications() {
        return notificationService.listAll();
    }

    @Override
    public CreditScoreResponse getUserScore(UUID userId) {
        return scoringService.getLatestScore(userId);
    }
}
