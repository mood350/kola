package com.kola.backend.modules.vault.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Amount to move for a vault deposit or withdrawal. Vault movements are free (KOLA.md 4.2). */
public record VaultOperationRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount) {
}
