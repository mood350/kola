package com.kola.backend.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO retourné après login ou register.
 * POURQUOI deux tokens ?
 *  - accessToken  → durée courte (15-30 min) → envoyé dans chaque requête
 *  - refreshToken → durée longue (7 jours)   → utilisé UNIQUEMENT pour
 *                   renouveler l'access token sans re-login
 * JsonProperty → contrôle le nom exact dans le JSON retourné
 */
@Getter
@Setter
@Builder
public class AuthenticationResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;
}