package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One saver: what a customer keeps in their savings account and in their vaults.
 *
 * <p>The vaults live in the customer's current account (their money is locked there), while the
 * savings account is a separate one — so {@code total} adds the two without counting anything twice.
 *
 * @param savings    balance of the savings account, available and locked together
 * @param collateral the locked part of it: what secures a running loan
 * @param vaults     active vaults
 * @param total      {@code savings + vaultsBalance}
 */
public record ConsoleSavingsRow(UUID userId,
                                String fullName,
                                String phone,
                                BigDecimal savings,
                                BigDecimal collateral,
                                long vaults,
                                BigDecimal vaultsBalance,
                                BigDecimal total) {
}
