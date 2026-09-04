package com.dogaa.backend.modules.kyc.dto;

import com.dogaa.backend.modules.kyc.entity.KycDocumentStatus;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;

import java.time.Instant;
import java.util.UUID;

/** Never exposes the storage key: the file is reachable only through the review endpoint. */
public record KycDocumentResponse(UUID id,
                                  KycDocumentType type,
                                  KycDocumentStatus status,
                                  String originalFilename,
                                  String contentType,
                                  long sizeBytes,
                                  String rejectionReason,
                                  Instant reviewedAt,
                                  Instant submittedAt) {
}
