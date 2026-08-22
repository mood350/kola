package com.kola.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = Instant.now().toEpochMilli() - start.toEpochMilli();

            log.info("{} {} {}ms {} {}",
                    request.getMethod(),
                    request.getRequestURI() + redactedQuery(request),
                    duration,
                    response.getStatus(),
                    request.getRemoteAddr());
        }
    }

    /**
     * Reconstruit la query string en conservant le NOM des paramètres (utile
     * au diagnostic) mais en masquant systématiquement leurs VALEURS.
     *
     * Le masquage est aveugle, et c'est délibéré : maintenir une liste de
     * paramètres « sensibles » revient à parier qu'on n'en oubliera jamais un.
     * C'est ce pari qui a fait écrire en clair, dans ces logs, les mots de
     * passe et les codes de réinitialisation de
     * POST /api/auth/reset-password?token=...&newPassword=...
     * Ces deux champs sont passés dans le corps de la requête, mais le filtre
     * ne doit plus jamais pouvoir servir de canal de fuite.
     */
    private String redactedQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return "";
        }

        StringBuilder redacted = new StringBuilder("?");
        for (String pair : query.split("&")) {
            if (redacted.length() > 1) {
                redacted.append('&');
            }
            int separator = pair.indexOf('=');
            if (separator >= 0) {
                redacted.append(pair, 0, separator).append("=***");
            } else {
                redacted.append(pair);
            }
        }
        return redacted.toString();
    }
}
