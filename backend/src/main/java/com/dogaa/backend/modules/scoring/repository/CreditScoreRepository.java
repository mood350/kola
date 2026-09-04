package com.dogaa.backend.modules.scoring.repository;

import com.dogaa.backend.modules.scoring.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {

    Optional<CreditScore> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
