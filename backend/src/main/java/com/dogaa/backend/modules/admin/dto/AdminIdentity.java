package com.dogaa.backend.modules.admin.dto;

import com.dogaa.backend.modules.admin.entity.AdminRole;

/**
 * Shape consumed as-is by the back-office (BACKEND.md 4.1). {@code role} serialises to its label.
 *
 * @param lastLoginAt the sign-in recorded on the account, already formatted ("Il y a 2 h"), or null
 *                    for an account that has never signed in. On {@code /auth/me} this is the
 *                    sign-in that opened the current session — not a previous one, which the
 *                    account does not keep; the profile screen labels it accordingly.
 */
public record AdminIdentity(String id,
                            String name,
                            String email,
                            AdminRole role,
                            String scope,
                            String lastLoginAt) {
}
