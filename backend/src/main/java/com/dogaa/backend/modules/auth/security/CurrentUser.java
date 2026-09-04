package com.dogaa.backend.modules.auth.security;

import com.dogaa.backend.common.enums.Role;

import java.util.UUID;

/**
 * Principal placed in the {@code SecurityContext} by {@link JwtAuthenticationFilter}.
 * Inject it with {@code @AuthenticationPrincipal CurrentUser currentUser}.
 */
public record CurrentUser(UUID id, String phone, Role role) {
}
