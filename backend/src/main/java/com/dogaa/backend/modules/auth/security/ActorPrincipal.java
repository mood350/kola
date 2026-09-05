package com.dogaa.backend.modules.auth.security;

import java.util.UUID;

/**
 * Whoever is behind a request: an app user, or a back-office administrator.
 *
 * <p>The two are different populations with different credentials — a customer signs in with a
 * phone and a PIN, a member of staff with an email and a password — but code that only needs to
 * know "who did this" (audit trails, reviewer stamps) should not care which.
 */
public interface ActorPrincipal {

    UUID id();

    /** Human-readable identity for audit entries: a masked phone, or an administrator's name. */
    String displayName();
}
