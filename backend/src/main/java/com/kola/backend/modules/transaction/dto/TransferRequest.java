package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * P2P transfer (KOLA.md 4.1). If {@code recipientPhone} belongs to a Kola user the transfer
 * is settled internally; otherwise it is paid out to that number via Mobile Money.
 */
public record TransferRequest(
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank String recipientPhone,
        @Size(max = 140) String description) {
}
