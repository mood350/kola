package com.dogaa.backend.modules.admin.security;

import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.auth.security.ActorPrincipal;

import java.util.UUID;

/**
 * Principal placed in the security context by an administrator's token.
 * Inject with {@code @AuthenticationPrincipal CurrentAdmin admin}.
 */
public record CurrentAdmin(UUID id, String email, String name, AdminRole role)
        implements ActorPrincipal {

    @Override
    public String displayName() {
        return name;
    }
}
