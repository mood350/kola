package com.kola.backend.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Requête de paiement marchand (scan QR ou saisie manuelle du code marchand).
 */
public record PayMerchantRequest(
        @NotNull(message = "Le wallet source est obligatoire")
        Long sourceWalletId,

        @NotBlank(message = "Le code marchand est obligatoire")
        String merchantCode,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        /**
         * Clé d'idempotence fournie par le client (un UUID par opération).
         * Optionnelle pour rester compatible avec les clients existants, mais
         * indispensable en pratique : sans elle, un double-tap ou un retry
         * réseau rejoue intégralement le mouvement d'argent.
         */
        String idempotencyKey
) {
}
