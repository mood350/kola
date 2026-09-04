package com.dogaa.backend.modules.kyc.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One uploaded piece of KYC evidence.
 *
 * <p>The file itself never lives in this row: only a storage key does, so the database can be
 * dumped, replicated and read by support staff without spreading copies of people's ID cards.
 */
@Entity
@Table(name = "kyc_documents", indexes = {
        @Index(name = "idx_kyc_documents_user", columnList = "user_id"),
        @Index(name = "idx_kyc_documents_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocument extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KycDocumentType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KycDocumentStatus status = KycDocumentStatus.PENDING;

    /** Opaque handle understood by the configured {@code DocumentStorage}. */
    @Column(name = "storage_key", nullable = false, length = 200)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** Shown to the user so a rejected upload can be corrected rather than guessed at. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public boolean isApproved() {
        return status == KycDocumentStatus.APPROVED;
    }
}
