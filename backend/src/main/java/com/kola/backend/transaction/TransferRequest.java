package com.kola.backend.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Requête de transfert d'argent vers un bénéficiaire (interne Kola ou
 * externe via Mobile Money). Le bénéficiaire doit avoir été préalablement
 * enregistré via /api/beneficiaries.
 */
public record TransferRequest(
        @NotNull(message = "Le wallet source est obligatoire")
        Long sourceWalletId,

        @NotNull(message = "Le bénéficiaire est obligatoire")
        Long beneficiaryId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        String description,

        /**
         * Clé d'idempotence fournie par le client (un UUID par opération).
         * Optionnelle pour rester compatible avec les clients existants, mais
         * indispensable en pratique : sans elle, un double-tap ou un retry
         * réseau rejoue intégralement le mouvement d'argent.
         */
        String idempotencyKey
) {
}
