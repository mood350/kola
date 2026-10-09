package com.kola.backend.common.enums;

/**
 * Recurrence mode for a scheduled task (spec KOLA.md §4.6.2).
 */
public enum ScheduleFrequency {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY
}
