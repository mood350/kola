package com.kola.backend.modules.qr.dto;

import com.kola.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Paying a scanned code.
 *
 * <p>Both {@code amount} and {@code currency} are required for a STATIC code, which carries
 * neither. On a PAYMENT_REQUEST they are optional, and a value that contradicts the code is
 * <em>refused</em> rather than ignored: a payer who typed a number and got charged another one has
 * been lied to, even when the difference is in their favour.
 */
public record QrPaymentRequest(
        Currency currency,

        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @Size(max = 140) String description) {
}
