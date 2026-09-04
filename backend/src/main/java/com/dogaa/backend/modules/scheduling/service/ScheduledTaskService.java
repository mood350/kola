package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduledTaskService {

    ScheduledTaskResponse create(ScheduledTaskRequest request);

    ScheduledTaskResponse pause(UUID taskId);

    ScheduledTaskResponse resume(UUID taskId);

    ScheduledTaskResponse cancel(UUID taskId);

    List<ScheduledTaskResponse> listByUser(UUID userId);

    /**
     * Consumed by dogaa-admin (same module owner).
     */
    List<ScheduledTaskResponse> listAll();
}
