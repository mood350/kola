package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** Identity and account state. No PIN hash, no token — nothing a support screen has to hold. */
public record ConsoleUserProfile(UUID id,
                                 String fullName,
                                 String phone,
                                 String email,
                                 String city,
                                 String country,
                                 KycTier kycTier,
                                 UserStatus status,
                                 boolean locked,
                                 Instant lockedUntil,
                                 Instant createdAt,
                                 Instant lastLoginAt) {
}
