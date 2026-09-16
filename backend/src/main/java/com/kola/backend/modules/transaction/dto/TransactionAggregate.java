package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.Currency;

import java.math.BigDecimal;

/**
 * Per-currency transaction totals for the admin back-office (KOLA.md 4.5:
 * "Volume de transactions"). Amounts of different currencies are never
 * summed together, so this is grouped by {@link Currency}.
 */
public record TransactionAggregate(Currency currency, long count, BigDecimal totalAmount, BigDecimal totalFees) {
}
