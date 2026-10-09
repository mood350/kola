package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;

/**
 * Totals of the loans matching the current filters.
 *
 * @param principal   what was lent
 * @param outstanding what those loans still owe today
 * @param overdue     how many of them are late
 */
public record ConsoleLoanSummary(long count, BigDecimal principal, BigDecimal outstanding, long overdue) {
}
