package com.kola.backend.modules.scoring.repository;

import com.kola.backend.modules.scoring.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditScoreRepository extends JpaRepository<CreditScore, UUID> {

    Optional<CreditScore> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
