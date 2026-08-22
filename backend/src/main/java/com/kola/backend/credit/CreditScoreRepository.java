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

    // clearAutomatically vide le cache JPA après l'UPDATE (les CreditScore en
    // mémoire ont un `latest` devenu faux).
    //
    // flushAutomatically est INDISPENSABLE avec lui : sans flush préalable, le
    // clear() jette les modifications encore en attente du contexte. C'est ce
    // qui cassait silencieusement LoanService.repay() — le débit du wallet et
    // le passage du prêt à REPAID étaient perdus, alors que la transaction de
    // remboursement (INSERT immédiat via @GeneratedValue IDENTITY) restait,
    // laissant un remboursement fantôme au grand livre.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CreditScore cs SET cs.latest = false WHERE cs.user.id = :userId")
    void markAllAsNotLatest(@Param("userId") Long userId);

    @Query("SELECT DISTINCT cs.user.id FROM CreditScore cs WHERE cs.latest = true")
    List<Long> findAllUserIdsWithLatestScore();
}