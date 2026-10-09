package com.kola.backend.modules.scheduling.service;

import com.kola.backend.common.enums.ScheduledTaskStatus;
import com.kola.backend.modules.notification.service.NotificationService;
import com.kola.backend.modules.scheduling.entity.ScheduledTask;
import com.kola.backend.modules.scheduling.repository.ScheduledTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Polling job (spec KOLA.md §4.6.3): checks every due, active task, executes
 * it via {@link TaskExecutionPort}, reschedules recurring tasks, and retries a
 * failure once ~24h later before marking the task FAILED.
 */
@Component
public class ScheduledTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskRunner.class);
    private static final int MAX_RETRIES = 1;

    private final ScheduledTaskRepository repository;
    private final TaskExecutionPort taskExecutionPort;
    private final NotificationService notificationService;
    private final ScheduleNextRunCalculator nextRunCalculator;

    public ScheduledTaskRunner(ScheduledTaskRepository repository,
                                TaskExecutionPort taskExecutionPort,
                                NotificationService notificationService,
                                ScheduleNextRunCalculator nextRunCalculator) {
        this.repository = repository;
        this.taskExecutionPort = taskExecutionPort;
        this.notificationService = notificationService;
        this.nextRunCalculator = nextRunCalculator;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.scheduled-transactions-poll-ms:60000}")
    @Transactional
    public void runDueTasks() {
        List<ScheduledTask> due = repository.findByStatusAndNextRunAtLessThanEqual(ScheduledTaskStatus.ACTIVE, Instant.now());
        log.info("Scheduled task run: {} due task(s)", due.size());
        due.forEach(this::process);
    }

    private void process(ScheduledTask task) {
        boolean success = taskExecutionPort.execute(task);

        if (success) {
            task.setOccurrencesCompleted(task.getOccurrencesCompleted() + 1);
            task.setRetryCount(0);
            task.setLastFailureReason(null);
            notificationService.notifyTaskOutcome(task.getUserId(), task.getId(), true, null);

            if (nextRunCalculator.isFinished(task)) {
                task.setStatus(ScheduledTaskStatus.COMPLETED);
            } else {
                task.setNextRunAt(nextRunCalculator.next(task));
            }
        } else {
            task.setRetryCount(task.getRetryCount() + 1);
            task.setLastFailureReason("Solde insuffisant ou restriction KYC");
            notificationService.notifyTaskOutcome(task.getUserId(), task.getId(), false, task.getLastFailureReason());

            if (task.getRetryCount() > MAX_RETRIES) {
                task.setStatus(ScheduledTaskStatus.FAILED);
            } else {
                task.setNextRunAt(Instant.now().plus(Duration.ofHours(24)));
            }
        }

        repository.save(task);
    }
}
