package com.kola.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Réinitialisation du mot de passe via le code reçu par email.
 *
 * Ces deux champs transitaient auparavant en query params
 * (POST /api/auth/reset-password?token=...&newPassword=...). Une URL n'est pas
 * un endroit confidentiel : elle est écrite telle quelle dans les logs
 * applicatifs (cf. RequestLoggingFilter), dans ceux de tout reverse proxy en
 * amont, et reste dans l'historique du navigateur. Le mot de passe en clair et
 * le code de réinitialisation — qui suffit à prendre le contrôle du compte —
 * y étaient donc archivés durablement. Ils passent désormais par le corps de
 * la requête.
 *
 * Les règles de robustesse sont volontairement identiques à celles de
 * RegistrationRequest : sans ça, ce flux devenait une porte dérobée pour poser
 * un mot de passe plus faible que ce que l'inscription autorise.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "Le code de réinitialisation est obligatoire")
        @Pattern(regexp = "^\\d{6}$", message = "Le code de réinitialisation doit contenir 6 chiffres")
        String token,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre"
        )
        String newPassword
) {
}
