package com.kola.backend.modules.vault.dto;

import com.kola.backend.common.enums.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Opens a new vault (KOLA.md 4.2). The owner must already hold a wallet in {@code currency};
 * that wallet funds the vault. {@code targetAmount} and {@code targetDate} are optional goals.
 */
public record CreateVaultRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull Currency currency,
        @Positive @Digits(integer = 17, fraction = 2) BigDecimal targetAmount,
        LocalDate targetDate,
        @Size(max = 160) String description) {
}
