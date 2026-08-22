package com.kola.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kola.backend.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Pendant de RestAuthenticationEntryPoint pour le cas "authentifié mais pas
 * les droits" (ex: un CLIENT qui appelle /api/admin/**) — même forme
 * ErrorResponse que le reste de l'API plutôt que le corps par défaut de
 * Spring Security.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new GlobalExceptionHandler.ErrorResponse(
                "ACCESS_DENIED",
                "Vous n'avez pas les droits pour accéder à cette ressource.",
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
