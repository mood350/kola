package com.dogaa.backend.modules.auth.security;

import com.dogaa.backend.common.enums.Role;
import com.dogaa.backend.common.util.PhoneNumbers;

import java.util.UUID;

/**
 * Principal placed in the {@code SecurityContext} by {@link JwtAuthenticationFilter}.
 * Inject it with {@code @AuthenticationPrincipal CurrentUser currentUser}.
 */
public record CurrentUser(UUID id, String phone, Role role) implements ActorPrincipal {

    /** Masked: an audit trail should identify someone without printing their number in full. */
    @Override
    public String displayName() {
        return PhoneNumbers.mask(phone);
    }
}
