package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.scheduling.mapper.ScheduledTaskMapper;
import com.dogaa.backend.modules.scheduling.repository.ScheduledTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduledTaskServiceImpl implements ScheduledTaskService {

    private final ScheduledTaskRepository repository;

    public ScheduledTaskServiceImpl(ScheduledTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public ScheduledTaskResponse create(ScheduledTaskRequest request) {
        ScheduledTask task = new ScheduledTask();
        task.setUserId(request.userId());
        task.setType(request.type());
        task.setFrequency(request.frequency());
        task.setAmount(request.amount());
        task.setCurrency(request.currency());
        task.setBeneficiaryReference(request.beneficiaryReference());
        task.setNextRunAt(request.firstRunAt());
        task.setEndDate(request.endDate());
        task.setMaxOccurrences(request.maxOccurrences());
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse pause(UUID taskId) {
        ScheduledTask task = findActiveOrPaused(taskId);
        task.setStatus(ScheduledTaskStatus.PAUSED);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse resume(UUID taskId) {
        ScheduledTask task = getOrThrow(taskId);
        if (task.getStatus() != ScheduledTaskStatus.PAUSED) {
            throw new ConflictException("Seule une tache en pause peut etre reprise.");
        }
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse cancel(UUID taskId) {
        ScheduledTask task = findActiveOrPaused(taskId);
        task.setStatus(ScheduledTaskStatus.CANCELLED);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskResponse> listByUser(UUID userId) {
        return repository.findByUserIdOrderByNextRunAtAsc(userId).stream()
                .map(ScheduledTaskMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledTaskResponse> listAll() {
        return repository.findAll().stream()
                .map(ScheduledTaskMapper::toResponse)
                .toList();
    }

    private ScheduledTask findActiveOrPaused(UUID taskId) {
        ScheduledTask task = getOrThrow(taskId);
        if (task.getStatus() != ScheduledTaskStatus.ACTIVE && task.getStatus() != ScheduledTaskStatus.PAUSED) {
            throw new ConflictException("Cette tache ne peut plus etre modifiee (statut: " + task.getStatus() + ").");
        }
        return task;
    }

    private ScheduledTask getOrThrow(UUID taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tache programmee introuvable: " + taskId));
    }
}
