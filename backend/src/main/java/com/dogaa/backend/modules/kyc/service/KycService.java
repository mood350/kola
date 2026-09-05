package com.dogaa.backend.modules.kyc.service;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.config.KycProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.auth.security.ActorPrincipal;
import com.dogaa.backend.modules.kyc.dto.KycDocumentResponse;
import com.dogaa.backend.modules.kyc.dto.KycStatusResponse;
import com.dogaa.backend.modules.kyc.dto.ReviewDocumentRequest;
import com.dogaa.backend.modules.kyc.entity.KycDocument;
import com.dogaa.backend.modules.kyc.entity.KycDocumentStatus;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.kyc.mapper.KycDocumentMapper;
import com.dogaa.backend.modules.kyc.repository.KycDocumentRepository;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.event.UserProfileUpdatedEvent;
import com.dogaa.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Progressive verification (DOGAA.md 4.4).
 *
 * <p>The tier is never written directly. Every path that could raise or lower it ends in
 * {@link #recomputeTier}, which asks {@link KycTierRules} what the user's verified facts add up to.
 * A revoked document therefore demotes the account for free, and no endpoint can hand out a tier
 * by mistake.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final KycDocumentMapper kycDocumentMapper;
    private final KycLimitService kycLimitService;
    private final DocumentStorage documentStorage;
    private final UserService userService;
    private final KycProperties kycProperties;
    private final AuditService auditService;

    // ------------------------------------------------------------------ status

    @Transactional(readOnly = true)
    public KycStatusResponse getStatus(UUID userId) {
        User user = userService.getById(userId);
        List<KycDocument> documents = kycDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Set<KycDocumentType> approved = approvedTypes(documents);

        return new KycStatusResponse(
                user.getKycTier(),
                KycTierRules.nextTier(user.getKycTier()),
                user.isPhoneVerified(),
                KycTierRules.isProfileComplete(user),
                approved.stream().anyMatch(KycDocumentType::isIdentityDocument),
                KycTierRules.requirementsForNextTier(user, approved),
                kycDocumentMapper.toResponse(kycLimitService.limitsFor(user.getKycTier())),
                documents.stream().map(kycDocumentMapper::toResponse).toList());
    }

    /**
     * The declarative half of the ladder lives on the profile, so a profile edit can promote or
     * demote a user. Reacting to the event keeps the user module free of any KYC import.
     */
    @EventListener
    @Transactional
    public void onProfileUpdated(UserProfileUpdatedEvent event) {
        recomputeTier(userService.getById(event.userId()));
    }

    // ------------------------------------------------ tiers 2 and 3: documents

    @Transactional
    public KycDocumentResponse submitDocument(UUID userId, KycDocumentType type, MultipartFile file) {
        validate(file);

        // One pending upload per type: re-submitting replaces the previous attempt rather than
        // queueing a second one for the reviewer.
        kycDocumentRepository
                .findFirstByUserIdAndTypeAndStatusOrderByCreatedAtDesc(userId, type, KycDocumentStatus.PENDING)
                .ifPresent(previous -> {
                    documentStorage.delete(previous.getStorageKey());
                    kycDocumentRepository.delete(previous);
                });
        if (kycDocumentRepository.existsByUserIdAndTypeAndStatus(userId, type, KycDocumentStatus.APPROVED)) {
            throw new ConflictException("A " + type + " has already been approved for this account");
        }

        KycDocument document = kycDocumentRepository.save(KycDocument.builder()
                .userId(userId)
                .type(type)
                .storageKey(documentStorage.store(file, userId.toString()))
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build());

        log.info("KYC document {} ({}) submitted by user {}", document.getId(), type, userId);
        return kycDocumentMapper.toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> listDocuments(UUID userId) {
        return kycDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(kycDocumentMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------ back-office

    @Transactional(readOnly = true)
    public Page<KycDocumentResponse> listPendingDocuments(Pageable pageable) {
        return kycDocumentRepository
                .findByStatusOrderByCreatedAtAsc(KycDocumentStatus.PENDING, pageable)
                .map(kycDocumentMapper::toResponse);
    }

    /** Streams the file to a reviewer. The only path by which a stored document leaves the server. */
    @Transactional(readOnly = true)
    public StoredFile downloadDocument(UUID documentId) {
        KycDocument document = getDocument(documentId);
        return new StoredFile(documentStorage.load(document.getStorageKey()),
                document.getContentType(), document.getOriginalFilename());
    }

    @Transactional
    public KycDocumentResponse review(UUID documentId, ActorPrincipal reviewer, ReviewDocumentRequest request) {
        UUID reviewerId = reviewer.id();
        KycDocument document = getDocument(documentId);

        if (document.getStatus() != KycDocumentStatus.PENDING) {
            throw new ConflictException("This document has already been reviewed");
        }
        if (!Boolean.TRUE.equals(request.approved())
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new BadRequestException("A rejection must state a reason");
        }

        document.setStatus(Boolean.TRUE.equals(request.approved())
                ? KycDocumentStatus.APPROVED
                : KycDocumentStatus.REJECTED);
        document.setRejectionReason(Boolean.TRUE.equals(request.approved())
                ? null
                : request.rejectionReason().trim());
        document.setReviewedBy(reviewerId);
        document.setReviewedAt(Instant.now());
        kycDocumentRepository.save(document);

        log.info("KYC document {} {} by {}", documentId, document.getStatus(), reviewerId);

        User owner = userService.getById(document.getUserId());
        KycTier before = owner.getKycTier();
        KycTier after = recomputeTier(owner);

        auditService.record(reviewer, "users",
                (document.isApproved() ? "Approbation" : "Rejet") + " du document "
                        + document.getType() + " de " + owner.getFullName(),
                before == after ? null : AuditService.diff(before, after),
                "KycDocument", documentId.toString());

        return kycDocumentMapper.toResponse(document);
    }

    /**
     * Withdraws an approval, for instance when a document turns out to be forged. The demotion
     * follows on its own because the tier is derived rather than stored as a decision.
     */
    @Transactional
    public KycDocumentResponse revokeApproval(UUID documentId, ActorPrincipal reviewer, String reason) {
        UUID reviewerId = reviewer.id();
        KycDocument document = getDocument(documentId);

        if (!document.isApproved()) {
            throw new ConflictException("This document is not approved");
        }
        document.setStatus(KycDocumentStatus.REJECTED);
        document.setRejectionReason(reason);
        document.setReviewedBy(reviewerId);
        document.setReviewedAt(Instant.now());
        kycDocumentRepository.save(document);

        User user = userService.getById(document.getUserId());
        KycTier before = user.getKycTier();
        recomputeTier(user);
        log.warn("Approval revoked on document {} by {}: user {} moved from {} to {}",
                documentId, reviewerId, user.getId(), before, user.getKycTier());

        auditService.record(reviewer, "users",
                "Révocation du document " + document.getType() + " de " + user.getFullName()
                        + " — " + reason,
                AuditService.diff(before, user.getKycTier()),
                "KycDocument", documentId.toString());

        return kycDocumentMapper.toResponse(document);
    }

    // ----------------------------------------------------------------- shared

    /**
     * Recomputes the tier from the verified facts and persists it if it moved.
     *
     * @return the tier the user now holds
     */
    @Transactional
    public KycTier recomputeTier(User user) {
        Set<KycDocumentType> approved = approvedTypes(
                kycDocumentRepository.findByUserIdAndStatus(user.getId(), KycDocumentStatus.APPROVED));

        KycTier resolved = KycTierRules.resolve(user, approved);
        if (resolved != user.getKycTier()) {
            log.info("User {} moves from {} to {}", user.getId(), user.getKycTier(), resolved);
            user.setKycTier(resolved);
            userService.save(user);
        }
        return resolved;
    }

    private KycDocument getDocument(UUID documentId) {
        return kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private static Set<KycDocumentType> approvedTypes(List<KycDocument> documents) {
        Set<KycDocumentType> types = documents.stream()
                .filter(KycDocument::isApproved)
                .map(KycDocument::getType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(KycDocumentType.class)));
        return types;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The document file is required");
        }
        KycProperties.Upload upload = kycProperties.getUpload();
        if (file.getSize() > upload.getMaxFileSize().toBytes()) {
            throw new BadRequestException("The document must be smaller than "
                    + upload.getMaxFileSize().toMegabytes() + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !upload.getAllowedContentTypes().contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file type. Allowed: "
                    + String.join(", ", upload.getAllowedContentTypes()));
        }
    }

    /** A file on its way to a reviewer. */
    public record StoredFile(byte[] content, String contentType, String filename) {
    }
}
