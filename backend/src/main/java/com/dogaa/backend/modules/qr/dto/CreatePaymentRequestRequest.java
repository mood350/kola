package com.dogaa.backend.modules.qr.dto;

import com.dogaa.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Opens a claim for a precise amount.
 *
 * <p>No beneficiary field: the code is always payable to the caller. A QR whose beneficiary came
 * from the request body would let anyone print a code that credits someone else.
 *
 * @param expiresInMinutes null falls back to {@code app.qr.default-request-ttl}
 */
public record CreatePaymentRequestRequest(
        @NotNull Currency currency,

        @NotNull
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @Size(max = 140) String label,

        Integer expiresInMinutes) {
}
