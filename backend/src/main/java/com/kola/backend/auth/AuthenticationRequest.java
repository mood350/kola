package com.kola.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO reçu lors de la connexion.
 * Uniquement email + password — pas besoin du reste.
 */
@Getter
@Setter
@NoArgsConstructor  // ← AJOUT
@AllArgsConstructor
public class AuthenticationRequest {

    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}