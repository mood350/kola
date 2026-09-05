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
    public ScheduledTaskResponse create(UUID ownerId, ScheduledTaskRequest request) {
        ScheduledTask task = new ScheduledTask();
        // The owner comes from the token, not from the payload: trusting the body
        // would let anyone schedule a transfer out of someone else's wallet.
        task.setUserId(ownerId);
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
    public ScheduledTaskResponse pause(UUID ownerId, UUID taskId) {
        ScheduledTask task = requireOwner(ownerId, findActiveOrPaused(taskId));
        task.setStatus(ScheduledTaskStatus.PAUSED);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse resume(UUID ownerId, UUID taskId) {
        ScheduledTask task = requireOwner(ownerId, getOrThrow(taskId));
        if (task.getStatus() != ScheduledTaskStatus.PAUSED) {
            throw new ConflictException("Seule une tache en pause peut etre reprise.");
        }
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    @Override
    public ScheduledTaskResponse cancel(UUID ownerId, UUID taskId) {
        ScheduledTask task = requireOwner(ownerId, findActiveOrPaused(taskId));
        task.setStatus(ScheduledTaskStatus.CANCELLED);
        return ScheduledTaskMapper.toResponse(repository.save(task));
    }

    /**
     * Someone else's task answers exactly like one that does not exist. Saying "forbidden" would
     * confirm the id is real, which is itself information the caller has no business having.
     */
    private ScheduledTask requireOwner(UUID ownerId, ScheduledTask task) {
        if (!task.getUserId().equals(ownerId)) {
            throw new ResourceNotFoundException("Tache programmee introuvable : " + task.getId());
        }
        return task;
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
