package com.kola.backend.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Requête de création d'un nouveau wallet (multi-devise).
 */
public record CreateWalletRequest(
        @NotBlank(message = "La devise est obligatoire")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Le code devise doit être au format ISO 4217, ex: XOF")
        String currency
) {
}
