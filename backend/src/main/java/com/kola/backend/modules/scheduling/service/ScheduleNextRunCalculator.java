package com.kola.backend.modules.scheduling.service;

import com.kola.backend.modules.scheduling.entity.ScheduledTask;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
            case MONTHLY -> nextMonthly(task);
            case ONCE -> task.getNextRunAt();
        };
    }

    /**
     * The same day of the month, every month.
     *
     * <p>This used to add 30 days, which is not a month: a rent standing order set for the 15th
     * arrived on the 14th, then the 16th, and after a year had wandered by the better part of a
     * week. A user who picks the 15th because that is the day after payday means the 15th.
     *
     * <p>The chosen day is stored on the task rather than read back from the last run, so a short
     * month cannot shorten the series permanently: the 31st becomes the 28th in February and
     * returns to the 31st in March, instead of every later run inheriting the 28th. A run that
     * happened late — a retry the next morning — likewise does not drag the whole schedule with
     * it.
     */
    private Instant nextMonthly(ScheduledTask task) {
        LocalDateTime current = LocalDateTime.ofInstant(task.getNextRunAt(), ZoneOffset.UTC);
        int day = task.getDayOfMonth() == null ? current.getDayOfMonth() : task.getDayOfMonth();

        LocalDate nextMonth = current.toLocalDate().withDayOfMonth(1).plusMonths(1);
        int dayThatMonth = Math.min(day, nextMonth.lengthOfMonth());

        return nextMonth.withDayOfMonth(dayThatMonth)
                .atTime(current.toLocalTime())
                .toInstant(ZoneOffset.UTC);
    }

    private boolean hasReachedEnd(ScheduledTask task) {
        boolean pastEndDate = task.getEndDate() != null && task.getNextRunAt().isAfter(task.getEndDate());
        boolean reachedMaxOccurrences = task.getMaxOccurrences() != null
                && task.getOccurrencesCompleted() >= task.getMaxOccurrences();
        return pastEndDate || reachedMaxOccurrences;
    }
}
