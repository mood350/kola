package com.kola.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Demande d'envoi d'un code de réinitialisation.
 *
 * L'email n'est pas un secret, mais c'est une donnée personnelle : le laisser
 * en query param revenait à constituer, dans les logs, la liste horodatée des
 * clients ayant perdu leur mot de passe. Même traitement que les autres
 * endpoints du flux, par cohérence.
 */
public record ForgotPasswordRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email
) {
}
