package com.kola.backend.credit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    List<LoanRequest> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    // Vérifie l'existence d'un prêt actif (bloque toute nouvelle demande)
    @Query("SELECT COUNT(l) > 0 FROM LoanRequest l WHERE l.borrower.id = :userId AND l.status IN ('APPROVED', 'DISBURSED')")
    boolean hasActiveLoan(@Param("userId") Long userId);

    // Prêts en défaut potentiel (disbursed et date dépassée) — pour le job batch
    @Query("SELECT l FROM LoanRequest l WHERE l.status = 'DISBURSED' AND l.dueDate < CURRENT_DATE")
    List<LoanRequest> findOverdueLoans();

    Optional<LoanRequest> findFirstByBorrowerIdAndStatusIn(Long borrowerId, List<LoanStatus> statuses);
}
