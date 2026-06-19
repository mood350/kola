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
            String query = request.getQueryString() != null ? "?" + request.getQueryString() : "";

            log.info("{} {} {}ms {} {}",
                    request.getMethod(),
                    request.getRequestURI() + query,
                    duration,
                    response.getStatus(),
                    request.getRemoteAddr());
        }
    }
}
