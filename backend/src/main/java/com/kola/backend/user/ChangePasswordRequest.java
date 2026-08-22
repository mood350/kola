package com.kola.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changement de mot de passe depuis l'application (utilisateur déjà connecté).
 * Distinct du flux "mot de passe oublié" de AuthenticationService, qui passe
 * par un OTP envoyé par email. Ici on exige l'ancien mot de passe : sans ça,
 * un téléphone déverrouillé ou un token volé suffirait à voler le compte.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre"
        )
        String newPassword
) {
}
