package com.kola.backend.scheduler;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Requête de création d'un virement programmé (récurrent).
 *
 * `executionDay` s'interprète selon la fréquence :
 *  - MONTHLY : jour du mois (1-31, ramené au dernier jour pour les mois courts)
 *  - WEEKLY  : jour de la semaine (1 = lundi ... 7 = dimanche)
 * La borne 1-31 est validée ici ; la cohérence avec WEEKLY (1-7) est vérifiée
 * côté service, car elle dépend de la fréquence choisie.
 */
public record CreateScheduledTransferRequest(
        @NotNull(message = "Le wallet source est obligatoire")
        Long walletId,

        /** Coffre à alimenter. Null = simple débit récurrent du wallet. */
        Long targetVaultId,

        @NotNull(message = "La fréquence est obligatoire")
        ScheduledTransfer.Frequency frequency,

        @NotNull(message = "Le jour d'exécution est obligatoire")
        @Min(value = 1, message = "Le jour d'exécution doit être au minimum 1")
        @Max(value = 31, message = "Le jour d'exécution doit être au maximum 31")
        Integer executionDay,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        String description
) {
}
