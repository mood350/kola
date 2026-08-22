package com.kola.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kola.backend.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sans ce bean, Spring Security utilise son Http403ForbiddenEntryPoint par
 * défaut pour toute requête non authentifiée (token absent/invalide/expiré) :
 * un 403 SANS corps JSON, court-circuitant GlobalExceptionHandler puisque ça
 * se joue au niveau Filter, avant d'atteindre le contrôleur. Le mobile ne
 * pouvait alors afficher que "403" brut. Ici on renvoie la même forme
 * ErrorResponse{code,message,details,path,timestamp} que le reste de l'API,
 * avec un vrai 401 (authentification manquante/invalide, distinct du 403
 * "authentifié mais pas les droits" — cf. RestAccessDeniedHandler).
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new GlobalExceptionHandler.ErrorResponse(
                "UNAUTHENTICATED",
                "Authentification requise ou session expirée. Veuillez vous reconnecter.",
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
