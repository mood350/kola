package com.kola.backend.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalRequest(
        @NotNull(message = "Le wallet source est obligatoire")
        Long walletId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
        BigDecimal amount
) {
}
