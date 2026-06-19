package com.kola.backend.credit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {

    Optional<CreditScore> findByUserIdAndLatestTrue(Long userId);

    List<CreditScore> findByUserIdOrderByCreatedAtDesc(Long userId);

    // CORRECTION : Ajout de clearAutomatically = true pour vider le cache JPA après l'UPDATE
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditScore cs SET cs.latest = false WHERE cs.user.id = :userId")
    void markAllAsNotLatest(@Param("userId") Long userId);

    @Query("SELECT DISTINCT cs.user.id FROM CreditScore cs WHERE cs.latest = true")
    List<Long> findAllUserIdsWithLatestScore();
}