package com.dogaa.backend.modules.scheduling;

import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import com.dogaa.backend.modules.scheduling.service.ScheduleNextRunCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A monthly schedule used to advance by 30 days, which is not a month. A standing order set for
 * the 15th arrived on the 14th, then the 16th, and after a year had wandered by most of a week.
 */
class MonthlyRecurrenceTest {

    private final ScheduleNextRunCalculator calculator = new ScheduleNextRunCalculator();

    private ScheduledTask monthly(String nextRun, Integer dayOfMonth) {
        ScheduledTask task = new ScheduledTask();
        task.setFrequency(ScheduleFrequency.MONTHLY);
        task.setNextRunAt(Instant.parse(nextRun));
        task.setDayOfMonth(dayOfMonth);
        return task;
    }

    @Test
    void theSameDayComesBackEveryMonth() {
        assertThat(calculator.next(monthly("2026-01-15T08:00:00Z", 15)))
                .isEqualTo(Instant.parse("2026-02-15T08:00:00Z"));
        assertThat(calculator.next(monthly("2026-02-15T08:00:00Z", 15)))
                .isEqualTo(Instant.parse("2026-03-15T08:00:00Z"));
    }

    @Test
    void theTimeOfDayIsPreserved() {
        assertThat(calculator.next(monthly("2026-01-15T06:30:00Z", 15)))
                .isEqualTo(Instant.parse("2026-02-15T06:30:00Z"));
    }

    /** February has no 31st. The payment lands on the last day it has. */
    @Test
    void aDayThatDoesNotExistFallsOnTheLastDayOfTheMonth() {
        assertThat(calculator.next(monthly("2026-01-31T08:00:00Z", 31)))
                .isEqualTo(Instant.parse("2026-02-28T08:00:00Z"));
    }

    /**
     * And then goes back. Deriving the day from the last run instead of storing it would leave
     * every later payment stuck on the 28th because February once shortened it.
     */
    @Test
    void theChosenDayReturnsAfterAShortMonth() {
        assertThat(calculator.next(monthly("2026-02-28T08:00:00Z", 31)))
                .isEqualTo(Instant.parse("2026-03-31T08:00:00Z"));
    }

    @Test
    void the30thSurvivesFebruaryToo() {
        assertThat(calculator.next(monthly("2026-01-30T08:00:00Z", 30)))
                .isEqualTo(Instant.parse("2026-02-28T08:00:00Z"));
        assertThat(calculator.next(monthly("2026-02-28T08:00:00Z", 30)))
                .isEqualTo(Instant.parse("2026-03-30T08:00:00Z"));
    }

    @Test
    void aLeapYearGivesFebruaryItsTwentyNinth() {
        assertThat(calculator.next(monthly("2028-01-31T08:00:00Z", 31)))
                .isEqualTo(Instant.parse("2028-02-29T08:00:00Z"));
    }

    @Test
    void decemberRollsIntoTheNextYear() {
        assertThat(calculator.next(monthly("2026-12-05T08:00:00Z", 5)))
                .isEqualTo(Instant.parse("2027-01-05T08:00:00Z"));
    }

    /** An older row with no stored day still behaves: the day comes from the run it is on. */
    @Test
    void aScheduleWithoutAStoredDayFallsBackOnItsCurrentRun() {
        assertThat(calculator.next(monthly("2026-01-15T08:00:00Z", null)))
                .isEqualTo(Instant.parse("2026-02-15T08:00:00Z"));
    }
}
