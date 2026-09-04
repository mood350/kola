package com.dogaa.backend.modules.kyc.repository;

import com.dogaa.backend.modules.kyc.entity.KycDocument;
import com.dogaa.backend.modules.kyc.entity.KycDocumentStatus;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<KycDocument> findByUserIdAndStatus(UUID userId, KycDocumentStatus status);

    Optional<KycDocument> findFirstByUserIdAndTypeAndStatusOrderByCreatedAtDesc(
            UUID userId, KycDocumentType type, KycDocumentStatus status);

    boolean existsByUserIdAndTypeAndStatus(UUID userId, KycDocumentType type, KycDocumentStatus status);

    Page<KycDocument> findByStatusOrderByCreatedAtAsc(KycDocumentStatus status, Pageable pageable);
}
