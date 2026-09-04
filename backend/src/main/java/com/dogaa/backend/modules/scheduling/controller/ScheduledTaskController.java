package com.dogaa.backend.modules.scheduling.controller;

import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduling/tasks")
@Tag(name = "Scheduling", description = "Recurring and one-off scheduled transactions (DOGAA.md 4.6)")
@SecurityRequirement(name = "bearerAuth")
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;

    public ScheduledTaskController(ScheduledTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a scheduled task (P2P transfer, merchant payment, vault deposit or bill payment)")
    public ScheduledTaskResponse create(@Valid @RequestBody ScheduledTaskRequest request) {
        return taskService.create(request);
    }

    @PatchMapping("/{id}/pause")
    @Operation(summary = "Pause a recurring task without cancelling it")
    public ScheduledTaskResponse pause(@PathVariable UUID id) {
        return taskService.pause(id);
    }

    @PatchMapping("/{id}/resume")
    @Operation(summary = "Resume a paused task")
    public ScheduledTaskResponse resume(@PathVariable UUID id) {
        return taskService.resume(id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a task before its next execution")
    public ScheduledTaskResponse cancel(@PathVariable UUID id) {
        return taskService.cancel(id);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "List a user's scheduled tasks")
    public List<ScheduledTaskResponse> listByUser(@PathVariable UUID userId) {
        return taskService.listByUser(userId);
    }
}
