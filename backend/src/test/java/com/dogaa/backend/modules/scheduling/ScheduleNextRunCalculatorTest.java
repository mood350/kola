package com.dogaa.backend.modules.scheduling;

import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.scheduling.service.ScheduleNextRunCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleNextRunCalculatorTest {

    private final ScheduleNextRunCalculator calculator = new ScheduleNextRunCalculator();

    private static ScheduledTask task(ScheduleFrequency frequency, Instant nextRunAt) {
        ScheduledTask task = new ScheduledTask();
        task.setFrequency(frequency);
        task.setNextRunAt(nextRunAt);
        return task;
    }

    @Test
    void onceIsAlwaysFinishedAfterItsSingleRun() {
        assertThat(calculator.isFinished(task(ScheduleFrequency.ONCE, Instant.now()))).isTrue();
    }

    @Test
    void dailyAdvancesExactlyOneDay() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertThat(calculator.next(task(ScheduleFrequency.DAILY, now))).isEqualTo(now.plus(1, ChronoUnit.DAYS));
    }

    @Test
    void weeklyAdvancesExactlySevenDays() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertThat(calculator.next(task(ScheduleFrequency.WEEKLY, now))).isEqualTo(now.plus(7, ChronoUnit.DAYS));
    }

    @Test
    void monthlyRecurrenceWithNoEndConditionIsNeverFinished() {
        ScheduledTask task = task(ScheduleFrequency.MONTHLY, Instant.now());
        assertThat(calculator.isFinished(task)).isFalse();
    }

    @Test
    void monthlyRecurrenceFinishesOncePastItsEndDate() {
        ScheduledTask task = task(ScheduleFrequency.MONTHLY, Instant.parse("2026-06-01T00:00:00Z"));
        task.setEndDate(Instant.parse("2026-05-01T00:00:00Z"));
        assertThat(calculator.isFinished(task)).isTrue();
    }

    @Test
    void monthlyRecurrenceFinishesOnceMaxOccurrencesIsReached() {
        ScheduledTask task = task(ScheduleFrequency.MONTHLY, Instant.now());
        task.setMaxOccurrences(3);
        task.setOccurrencesCompleted(3);
        assertThat(calculator.isFinished(task)).isTrue();
    }

    @Test
    void monthlyRecurrenceIsNotFinishedBeforeReachingMaxOccurrences() {
        ScheduledTask task = task(ScheduleFrequency.MONTHLY, Instant.now());
        task.setMaxOccurrences(3);
        task.setOccurrencesCompleted(2);
        assertThat(calculator.isFinished(task)).isFalse();
    }
}
