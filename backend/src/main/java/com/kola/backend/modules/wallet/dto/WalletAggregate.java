package com.kola.backend.modules.wallet.dto;

import com.kola.backend.common.enums.Currency;

import java.math.BigDecimal;

/**
 * Per-currency wallet totals for the admin back-office (KOLA.md 4.5:
 * "solde global"). Amounts of different currencies are never summed
 * together, so this is grouped by {@link Currency}.
 */
public record WalletAggregate(Currency currency, long walletCount, BigDecimal totalAvailable, BigDecimal totalLocked) {
}
