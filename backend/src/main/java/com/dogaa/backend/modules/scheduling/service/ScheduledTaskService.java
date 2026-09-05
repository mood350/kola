package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.UpdateScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;

import java.util.List;
import java.util.UUID;

public interface ScheduledTaskService {

    /**
     * @param ownerId the authenticated caller; the task is created for them, never for the
     *                id the request body happens to carry
     */
    ScheduledTaskResponse create(UUID ownerId, ScheduledTaskRequest request);

    /**
     * Edits a schedule in place. Absent fields are left alone.
     *
     * <p>Only an ACTIVE or PAUSED schedule can change: a cancelled or finished one is history, and
     * history is not edited.
     */
    ScheduledTaskResponse update(UUID ownerId, UUID taskId, UpdateScheduledTaskRequest request);

    ScheduledTaskResponse pause(UUID ownerId, UUID taskId);

    ScheduledTaskResponse resume(UUID ownerId, UUID taskId);

    ScheduledTaskResponse cancel(UUID ownerId, UUID taskId);

    List<ScheduledTaskResponse> listByUser(UUID userId);

    /**
     * Consumed by dogaa-admin (same module owner).
     */
    List<ScheduledTaskResponse> listAll();
}
