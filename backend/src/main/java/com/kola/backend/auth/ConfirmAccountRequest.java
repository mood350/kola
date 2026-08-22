package com.kola.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Activation du compte via le code à 6 chiffres reçu par email.
 *
 * Même raisonnement que ResetPasswordRequest : le code d'activation est un
 * secret à usage unique, il n'a rien à faire dans une URL. L'endpoint passe
 * aussi de GET à POST — activer un compte modifie un état, et les URLs de
 * requêtes GET sont les plus exposées (historique, referrer, logs d'accès).
 *
 * Le lien envoyé par email pointe vers le frontend
 * (application.mailing.frontend.activation-url), pas vers cette route : c'est
 * le client qui soumet le code, aucun lien cliquable n'est cassé.
 */
public record ConfirmAccountRequest(

        @NotBlank(message = "Le code d'activation est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code d'activation doit contenir 6 chiffres")
        String token
) {
}
