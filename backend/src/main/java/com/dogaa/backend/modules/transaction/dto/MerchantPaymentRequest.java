package com.dogaa.backend.modules.transaction.dto;

import com.dogaa.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payment to a partner merchant (DOGAA.md 4.1). {@code merchantCode} is what the QR encodes
 * or the customer types — resolved to a merchant account by the service.
 */
public record MerchantPaymentRequest(
        @NotNull Currency currency,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank String merchantCode,
        @Size(max = 140) String description) {
}
