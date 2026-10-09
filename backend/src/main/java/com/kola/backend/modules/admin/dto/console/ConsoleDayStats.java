package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;

/**
 * One calendar day (UTC, Lomé time) — today up to now, or all of yesterday. The dashboard sets the
 * two side by side; the console computes the difference.
 *
 * @param loanAmount principal disbursed that day
 * @param fees       fees earned on that day's completed transactions
 * @param repayments loan repayments received that day
 */
public record ConsoleDayStats(long transactions,
                              BigDecimal volume,
                              long newUsers,
                              long loans,
                              BigDecimal loanAmount,
                              BigDecimal fees,
                              BigDecimal repayments) {
}
