package com.dogaa.backend.modules.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Reads {@code Authorization: Bearer <jwt>} and populates the security context. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(BEARER_PREFIX.length()).trim();

            jwtService.parseAccessToken(token).ifPresentOrElse(
                    currentUser -> authenticate(request, currentUser,
                            List.of(new SimpleGrantedAuthority(currentUser.role().authority()))),
                    () -> jwtService.parseAdminToken(token).ifPresent(admin -> {
                        // ROLE_ADMIN keeps the existing /api/v1/admin/** rule working; the module
                        // authorities carry the per-role matrix that the endpoints check.
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        admin.role().modules().forEach(
                                module -> authorities.add(new SimpleGrantedAuthority(module.authority())));
                        authenticate(request, admin, authorities);
                    }));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Object principal,
                              Collection<SimpleGrantedAuthority> authorities) {
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
