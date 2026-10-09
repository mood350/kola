package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;
import java.util.List;

/**
 * Totals of the transactions matching the current filters — what an accountant reads before
 * exporting. {@code fees} is what Kola earned on them.
 */
public record ConsoleTransactionSummary(long count,
                                        BigDecimal volume,
                                        BigDecimal fees,
                                        List<ConsoleTypeTotal> byType) {
}
