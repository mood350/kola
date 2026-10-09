package com.kola.backend.modules.notification;

import com.kola.backend.common.enums.NotificationChannel;
import com.kola.backend.modules.notification.dto.NotificationRequest;
import com.kola.backend.modules.notification.entity.Notification;
import com.kola.backend.modules.notification.entity.NotificationDeliveryStatus;
import com.kola.backend.modules.notification.provider.NotificationProvider;
import com.kola.backend.modules.notification.repository.NotificationRepository;
import com.kola.backend.modules.notification.service.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationProvider pushProvider;

    @Mock
    private NotificationProvider emailProvider;

    private NotificationServiceImpl service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(pushProvider.channel()).thenReturn(NotificationChannel.PUSH);
        when(emailProvider.channel()).thenReturn(NotificationChannel.EMAIL);
        service = new NotificationServiceImpl(repository, List.of(pushProvider, emailProvider));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void routesToTheProviderMatchingTheRequestedChannel() {
        when(pushProvider.send(any(Notification.class))).thenReturn(true);

        var response = service.send(new NotificationRequest(userId, NotificationChannel.PUSH, "Title", "Body"));

        assertThat(response.status()).isEqualTo(NotificationDeliveryStatus.SENT);
    }

    @Test
    void marksTheNotificationFailedWhenTheProviderReportsFailure() {
        when(emailProvider.send(any(Notification.class))).thenReturn(false);

        var response = service.send(new NotificationRequest(userId, NotificationChannel.EMAIL, "Title", "Body"));

        assertThat(response.status()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(response.sentAt()).isNull();
    }

    @Test
    void marksTheNotificationFailedWhenNoProviderHandlesTheChannel() {
        var response = service.send(new NotificationRequest(userId, NotificationChannel.SMS, "Title", "Body"));

        assertThat(response.status()).isEqualTo(NotificationDeliveryStatus.FAILED);
    }
}
