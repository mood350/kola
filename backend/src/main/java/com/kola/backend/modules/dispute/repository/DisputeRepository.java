package com.kola.backend.modules.dispute.repository;

import com.kola.backend.modules.dispute.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);

    List<Dispute> findAllByOrderByCreatedAtDesc();
}
