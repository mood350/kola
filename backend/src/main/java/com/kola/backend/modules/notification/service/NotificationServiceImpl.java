package com.kola.backend.modules.notification.service;

import com.kola.backend.exception.ResourceNotFoundException;
import com.kola.backend.modules.notification.dto.NotificationRequest;
import com.kola.backend.modules.notification.dto.NotificationResponse;
import com.kola.backend.modules.notification.entity.Notification;
import com.kola.backend.modules.notification.entity.NotificationDeliveryStatus;
import com.kola.backend.modules.notification.mapper.NotificationMapper;
import com.kola.backend.modules.notification.provider.NotificationProvider;
import com.kola.backend.modules.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final Map<com.kola.backend.common.enums.NotificationChannel, NotificationProvider> providersByChannel;

    public NotificationServiceImpl(NotificationRepository repository, List<NotificationProvider> providers) {
        this.repository = repository;
        this.providersByChannel = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::channel, Function.identity()));
    }

    @Override
    public NotificationResponse send(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setChannel(request.channel());
        notification.setTitle(request.title());
        notification.setBody(request.body());
        notification.setStatus(NotificationDeliveryStatus.PENDING);

        NotificationProvider provider = providersByChannel.get(request.channel());
        boolean delivered = provider != null && provider.send(notification);

        notification.setStatus(delivered ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED);
        notification.setSentAt(delivered ? Instant.now() : null);

        return NotificationMapper.toResponse(repository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listByUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listAll() {
        return repository.findAll().stream()
                .map(NotificationMapper::toResponse)
                .toList();
    }
}
