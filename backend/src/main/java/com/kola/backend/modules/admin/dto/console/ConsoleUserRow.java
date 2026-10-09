package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A customer in the console list. {@code locked} is the temporary PIN lockout, not a suspension.
 *
 * @param pendingDocuments identity documents waiting for review — shown next to the KYC tier so the
 *                         list says who is waiting on the back-office
 */
public record ConsoleUserRow(UUID id,
                             String fullName,
                             String phone,
                             KycTier kycTier,
                             UserStatus status,
                             boolean locked,
                             long pendingDocuments,
                             Instant createdAt,
                             Instant lastLoginAt) {
}
