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

    // Le score courant (latest = true) d'un utilisateur
    Optional<CreditScore> findByUserIdAndLatestTrue(Long userId);

    // Historique complet des scores d'un utilisateur (du plus récent au plus ancien)
    List<CreditScore> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Marque tous les scores d'un utilisateur comme non-latest avant d'en créer un nouveau
    @Modifying
    @Query("UPDATE CreditScore cs SET cs.latest = false WHERE cs.user.id = :userId")
    void markAllAsNotLatest(@Param("userId") Long userId);

    // Pour le job batch : récupère les IDs de tous les utilisateurs actifs à rescorer
    @Query("SELECT DISTINCT cs.user.id FROM CreditScore cs WHERE cs.latest = true")
    List<Long> findAllUserIdsWithLatestScore();
}
