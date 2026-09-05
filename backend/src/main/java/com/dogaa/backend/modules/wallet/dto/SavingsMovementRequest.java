package com.dogaa.backend.modules.wallet.dto;

import com.dogaa.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Moving money between one's own current and savings accounts.
 *
 * <p>No wallet ids and no user id: both accounts are resolved from the caller and the currency, so
 * there is no way to name someone else's wallet.
 */
public record SavingsMovementRequest(
        @NotNull Currency currency,

        @NotNull
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount) {
}
