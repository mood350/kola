package com.kola.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // ← AJOUT — constructeur vide pour Jackson
@AllArgsConstructor // ← AJOUT — constructeur complet pour @Builder
public class RegistrationRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstname;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastname;

    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    // BUG CORRIGÉ : ce champ était absent alors que User.phoneNumber est
    // nullable = false en BDD. L'inscription levait systématiquement une
    // DataIntegrityViolationException (NULL non autorisé) à l'insertion.
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(
            regexp = "^\\+[1-9]\\d{6,14}$",
            message = "Le numéro de téléphone doit être au format international, ex: +22890000000"
    )
    private String phoneNumber;

    // Code pays ISO 3166-1 alpha-2 (ex: "TG", "SN", "CI", "GH")
    @NotBlank(message = "Le code pays est obligatoire")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Le code pays doit être au format ISO 3166-1 alpha-2, ex: TG")
    private String countryCode;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre"
    )
    private String password;
}