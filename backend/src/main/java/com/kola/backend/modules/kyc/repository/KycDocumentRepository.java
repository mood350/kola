package com.kola.backend.modules.kyc.repository;

import com.kola.backend.modules.kyc.entity.KycDocument;
import com.kola.backend.modules.kyc.entity.KycDocumentStatus;
import com.kola.backend.modules.kyc.entity.KycDocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    List<KycDocument> findByStatusOrderByCreatedAtAsc(KycDocumentStatus status);

    long countByStatus(KycDocumentStatus status);

    /** Pending documents per user, for one page of the back-office user list. */
    @Query("""
            select d.userId, count(d) from KycDocument d
            where d.status = :status and d.userId in :userIds
            group by d.userId
            """)
    List<Object[]> countByUserIds(@Param("status") KycDocumentStatus status,
                                  @Param("userIds") Collection<UUID> userIds);
}
