package com.kola.backend.modules.dispute.repository;

import com.kola.backend.modules.dispute.entity.DisputeValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeValidationRepository extends JpaRepository<DisputeValidation, UUID> {

    long countByDisputeId(UUID disputeId);

    boolean existsByDisputeIdAndAdminId(UUID disputeId, UUID adminId);

    List<DisputeValidation> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
