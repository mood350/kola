package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cash-out to an external Mobile Money account (KOLA.md 4.1, 5.3.A) — charged ~1%. */
public record CashOutRequest(
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank String phoneNumber,
        @Size(max = 40) String provider) {
}
