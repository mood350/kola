package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ScheduleNextRunCalculator {

    public boolean isFinished(ScheduledTask task) {
        return switch (task.getFrequency()) {
            case ONCE -> true;
            case DAILY, WEEKLY, MONTHLY -> hasReachedEnd(task);
        };
    }

    public Instant next(ScheduledTask task) {
        return switch (task.getFrequency()) {
            case DAILY -> task.getNextRunAt().plus(1, ChronoUnit.DAYS);
            case WEEKLY -> task.getNextRunAt().plus(7, ChronoUnit.DAYS);
            case MONTHLY -> task.getNextRunAt().plus(30, ChronoUnit.DAYS);
            case ONCE -> task.getNextRunAt();
        };
    }

    private boolean hasReachedEnd(ScheduledTask task) {
        boolean pastEndDate = task.getEndDate() != null && task.getNextRunAt().isAfter(task.getEndDate());
        boolean reachedMaxOccurrences = task.getMaxOccurrences() != null
                && task.getOccurrencesCompleted() >= task.getMaxOccurrences();
        return pastEndDate || reachedMaxOccurrences;
    }
}
