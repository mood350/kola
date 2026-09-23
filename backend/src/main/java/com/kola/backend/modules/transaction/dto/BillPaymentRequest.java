package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Utility bill payment (KOLA.md 4.6.1: electricite, eau, forfait telecom,
 * remboursement de pret anticipe). {@code billerReference} is whatever the
 * invoice prints — biller code and subscriber/meter number combined (e.g.
 * "CEET-00234891") — resolved by the external gateway, not by Kola.
 */
public record BillPaymentRequest(
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank String billerReference,
        @Size(max = 140) String description) {
}
