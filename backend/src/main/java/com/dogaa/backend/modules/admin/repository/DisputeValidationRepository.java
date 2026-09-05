package com.dogaa.backend.modules.admin.repository;

import com.dogaa.backend.modules.admin.entity.DisputeValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeValidationRepository extends JpaRepository<DisputeValidation, UUID> {

    List<DisputeValidation> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);

    long countByDisputeId(UUID disputeId);

    boolean existsByDisputeIdAndAdminId(UUID disputeId, UUID adminId);
}
