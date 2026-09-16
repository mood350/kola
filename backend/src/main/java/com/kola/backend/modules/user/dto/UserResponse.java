package com.kola.backend.modules.user.dto;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.Role;
import com.kola.backend.common.enums.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Public view of a user. Never carries the PIN hash or the lockout counters. */
public record UserResponse(UUID id,
                           String firstName,
                           String lastName,
                           String phone,
                           String email,
                           LocalDate dateOfBirth,
                           String address,
                           String city,
                           String country,
                           boolean phoneVerified,
                           boolean emailVerified,
                           KycTier kycTier,
                           Role role,
                           UserStatus status,
                           Instant lastLoginAt,
                           Instant privacyPolicyAcceptedAt,
                           Instant createdAt) {
}
