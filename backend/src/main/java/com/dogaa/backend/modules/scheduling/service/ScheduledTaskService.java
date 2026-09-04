package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;

import java.util.List;

public interface ScheduledTaskService {

    ScheduledTaskResponse create(ScheduledTaskRequest request);

    ScheduledTaskResponse pause(Long taskId);

    ScheduledTaskResponse resume(Long taskId);

    ScheduledTaskResponse cancel(Long taskId);

    List<ScheduledTaskResponse> listByUser(Long userId);

    /**
     * Consumed by dogaa-admin (same module owner).
     */
    List<ScheduledTaskResponse> listAll();
}
