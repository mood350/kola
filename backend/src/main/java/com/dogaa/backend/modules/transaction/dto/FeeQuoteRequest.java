package com.dogaa.backend.modules.transaction.dto;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** "How much will this cost me?" — a fee preview before the user confirms (DOGAA.md 4.1). */
public record FeeQuoteRequest(
        @NotNull TransactionType type,
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
