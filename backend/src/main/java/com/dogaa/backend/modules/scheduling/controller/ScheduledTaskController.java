package com.dogaa.backend.modules.scheduling.controller;

import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
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

@RestController
@RequestMapping("/api/v1/scheduling/tasks")
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;

    public ScheduledTaskController(ScheduledTaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledTaskResponse create(@Valid @RequestBody ScheduledTaskRequest request) {
        return taskService.create(request);
    }

    @PatchMapping("/{id}/pause")
    public ScheduledTaskResponse pause(@PathVariable Long id) {
        return taskService.pause(id);
    }

    @PatchMapping("/{id}/resume")
    public ScheduledTaskResponse resume(@PathVariable Long id) {
        return taskService.resume(id);
    }

    @PatchMapping("/{id}/cancel")
    public ScheduledTaskResponse cancel(@PathVariable Long id) {
        return taskService.cancel(id);
    }

    @GetMapping("/users/{userId}")
    public List<ScheduledTaskResponse> listByUser(@PathVariable Long userId) {
        return taskService.listByUser(userId);
    }
}
