package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.modules.admin.dto.AdminDashboardResponse;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.notification.entity.NotificationDeliveryStatus;
import com.dogaa.backend.modules.notification.service.NotificationService;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.service.ScoringService;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private static final int USER_GROWTH_WINDOW_DAYS = 30;

    private final ScheduledTaskService scheduledTaskService;
    private final NotificationService notificationService;
    private final ScoringService scoringService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final WalletService walletService;

    public AdminServiceImpl(ScheduledTaskService scheduledTaskService,
                             NotificationService notificationService,
                             ScoringService scoringService,
                             UserService userService,
                             TransactionService transactionService,
                             WalletService walletService) {
        this.scheduledTaskService = scheduledTaskService;
        this.notificationService = notificationService;
        this.scoringService = scoringService;
        this.userService = userService;
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    @Override
    public AdminDashboardResponse getDashboard() {
        List<ScheduledTaskResponse> tasks = scheduledTaskService.listAll();
        List<NotificationResponse> notifications = notificationService.listAll();

        long active = tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.ACTIVE).count();
        long failed = tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.FAILED).count();
        long sent = notifications.stream().filter(n -> n.status() == NotificationDeliveryStatus.SENT).count();
        long notificationsFailed = notifications.stream().filter(n -> n.status() == NotificationDeliveryStatus.FAILED).count();

        Instant growthWindowStart = Instant.now().minus(USER_GROWTH_WINDOW_DAYS, ChronoUnit.DAYS);

        return new AdminDashboardResponse(
                userService.totalUsers(),
                userService.newUsersSince(growthWindowStart),
                transactionService.aggregateByCurrency(TransactionStatus.COMPLETED),
                walletService.aggregateByCurrency(),
                tasks.size(),
                active,
                failed,
                sent,
                notificationsFailed);
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
