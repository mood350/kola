package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;

/**
 * Savings across the whole customer base, for the figures above the savers' list. Raw values — the
 * console formats them and works out any average.
 *
 * @param savers     customers holding something in a savings account or an active vault
 * @param total      everything they hold, {@code savings + vaultsBalance}
 * @param savings    savings accounts, available and locked together
 * @param collateral the locked part of the savings accounts: what secures running loans
 * @param vaults     active vaults
 */
public record ConsoleSavingsSummary(long savers,
                                    BigDecimal total,
                                    BigDecimal savings,
                                    BigDecimal collateral,
                                    long vaults,
                                    BigDecimal vaultsBalance) {
}
