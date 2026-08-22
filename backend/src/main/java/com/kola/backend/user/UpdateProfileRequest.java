package com.kola.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Mise à jour des informations modifiables du profil.
 * L'email n'y figure volontairement pas : il sert d'identifiant de connexion
 * et son changement exigerait une re-vérification par email.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Le prénom est obligatoire")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        String lastName,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Le numéro de téléphone doit être au format international, ex: +22890000000"
        )
        String phoneNumber,

        /** Identifiant d'avatar prédéfini (ex: "avatar_03"). Null = inchangé. */
        String avatar
) {
}
