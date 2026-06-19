package com.kola.backend.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Requête de rechargement d'un wallet depuis Mobile Money.
 * NOTE : externalReference sera fourni par le webhook de l'opérateur dans
 * une implémentation complète ; ici on simule la confirmation synchrone.
 */
public record DepositRequest(
        @NotNull(message = "Le wallet de destination est obligatoire")
        Long walletId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        String externalReference,

        String idempotencyKey
) {
}
