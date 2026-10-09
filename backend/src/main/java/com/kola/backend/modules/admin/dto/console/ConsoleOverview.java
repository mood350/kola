package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;
import java.util.List;

/**
 * The handful of numbers the console opens on. Raw values — the console formats them — and only
 * what an operator can act on: people to verify, loans running late.
 *
 * @param volume30d   completed transactions of the last 30 days, all currencies pooled (XOF today)
 * @param outstanding what active and overdue loans still owe
 * @param today       today so far, for the day-against-day comparison
 * @param yesterday   the whole of yesterday
 * @param volumeByDay the last 14 days, oldest first, every day present — the dashboard chart
 */
public record ConsoleOverview(long users,
                              long newUsers30d,
                              long suspendedUsers,
                              long kycPending,
                              long transactions30d,
                              BigDecimal volume30d,
                              long activeLoans,
                              long overdueLoans,
                              BigDecimal outstanding,
                              ConsoleDayStats today,
                              ConsoleDayStats yesterday,
                              List<ConsoleDailyVolume> volumeByDay) {
}
