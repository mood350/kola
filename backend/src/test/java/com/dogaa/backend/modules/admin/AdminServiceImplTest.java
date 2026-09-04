package com.dogaa.backend.modules.admin;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.modules.admin.dto.AdminDashboardResponse;
import com.dogaa.backend.modules.admin.service.AdminServiceImpl;
import com.dogaa.backend.modules.notification.dto.NotificationResponse;
import com.dogaa.backend.modules.notification.entity.NotificationDeliveryStatus;
import com.dogaa.backend.modules.notification.service.NotificationService;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.scoring.service.ScoringService;
import com.dogaa.backend.modules.transaction.dto.TransactionAggregate;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.dto.WalletAggregate;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private ScheduledTaskService scheduledTaskService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ScoringService scoringService;
    @Mock
    private UserService userService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private WalletService walletService;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(scheduledTaskService, notificationService, scoringService,
                userService, transactionService, walletService);
    }

    private ScheduledTaskResponse task(ScheduledTaskStatus status) {
        return new ScheduledTaskResponse(UUID.randomUUID(), UUID.randomUUID(), ScheduledTaskType.P2P_TRANSFER,
                ScheduleFrequency.ONCE, BigDecimal.TEN, Currency.XOF, "+22890000000", status,
                Instant.now(), null, null, 0, 0, null);
    }

    private NotificationResponse notification(NotificationDeliveryStatus status) {
        return new NotificationResponse(UUID.randomUUID(), UUID.randomUUID(),
                com.dogaa.backend.common.enums.NotificationChannel.PUSH, "t", "b", status, Instant.now());
    }

    @Test
    void assemblesRealGroupeAMetricsAlongsideGroupeBOnes() {
        when(scheduledTaskService.listAll()).thenReturn(List.of(
                task(ScheduledTaskStatus.ACTIVE), task(ScheduledTaskStatus.FAILED)));
        when(notificationService.listAll()).thenReturn(List.of(
                notification(NotificationDeliveryStatus.SENT), notification(NotificationDeliveryStatus.FAILED)));
        when(userService.totalUsers()).thenReturn(42L);
        when(userService.newUsersSince(any())).thenReturn(7L);
        List<TransactionAggregate> volume = List.of(new TransactionAggregate(Currency.XOF, 3L, BigDecimal.valueOf(150000), BigDecimal.valueOf(2250)));
        when(transactionService.aggregateByCurrency(eq(TransactionStatus.COMPLETED))).thenReturn(volume);
        List<WalletAggregate> balance = List.of(new WalletAggregate(Currency.XOF, 5L, BigDecimal.valueOf(300000), BigDecimal.valueOf(50000)));
        when(walletService.aggregateByCurrency()).thenReturn(balance);

        AdminDashboardResponse dashboard = adminService.getDashboard();

        assertThat(dashboard.totalUsers()).isEqualTo(42L);
        assertThat(dashboard.newUsersLast30Days()).isEqualTo(7L);
        assertThat(dashboard.completedTransactionVolume()).isEqualTo(volume);
        assertThat(dashboard.globalBalance()).isEqualTo(balance);
        assertThat(dashboard.totalScheduledTasks()).isEqualTo(2L);
        assertThat(dashboard.activeScheduledTasks()).isEqualTo(1L);
        assertThat(dashboard.failedScheduledTasks()).isEqualTo(1L);
        assertThat(dashboard.totalNotificationsSent()).isEqualTo(1L);
        assertThat(dashboard.totalNotificationsFailed()).isEqualTo(1L);
    }
}
