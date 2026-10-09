package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.modules.kyc.entity.KycDocumentStatus;
import com.kola.backend.modules.kyc.entity.KycDocumentType;

import java.time.Instant;
import java.util.UUID;

/**
 * An identity document. The file itself is fetched separately, as an attachment, from
 * {@code GET /api/v1/admin/kyc/documents/{id}/file}.
 */
public record ConsoleKycDocument(UUID id,
                                 UUID userId,
                                 String userName,
                                 String phone,
                                 KycDocumentType type,
                                 KycDocumentStatus status,
                                 KycTier currentTier,
                                 String fileName,
                                 String contentType,
                                 Instant submittedAt,
                                 String rejectionReason) {
}
