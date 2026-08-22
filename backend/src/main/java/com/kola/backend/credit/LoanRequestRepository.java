package com.kola.backend.credit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {

    List<LoanRequest> findByBorrowerIdOrderByCreatedAtDesc(Long borrowerId);

    // Vérifie l'existence d'un prêt non soldé (bloque toute nouvelle demande).
    // DEFAULTED est inclus : sans lui, un emprunteur dont le prêt venait de
    // basculer en défaut au batch de 2h pouvait enchaîner une nouvelle demande
    // dans la foulée — le défaut le libérait de la règle « un seul prêt à la
    // fois » au lieu de l'en exclure.
    @Query("SELECT COUNT(l) > 0 FROM LoanRequest l WHERE l.borrower.id = :userId AND l.status IN ('APPROVED', 'DISBURSED', 'DEFAULTED')")
    boolean hasActiveLoan(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) > 0 FROM LoanRequest l WHERE l.borrower.id = :userId AND l.status = 'DEFAULTED'")
    boolean hasDefaultedLoan(@Param("userId") Long userId);

    // Prêts en défaut potentiel (disbursed et date dépassée) — pour le job batch
    @Query("SELECT l FROM LoanRequest l WHERE l.status = 'DISBURSED' AND l.dueDate < CURRENT_DATE")
    List<LoanRequest> findOverdueLoans();

    Optional<LoanRequest> findFirstByBorrowerIdAndStatusIn(Long borrowerId, List<LoanStatus> statuses);

    /* ---------------------------------------------------------------------
       Console d'administration
       ------------------------------------------------------------------ */

    /**
     * Liste paginée pour l'administration, emprunteur et score compris.
     *
     * LE `JOIN FETCH` EST OBLIGATOIRE, PAS UNE OPTIMISATION. `open-in-view` est
     * désactivé et `borrower` comme `creditScoreSnapshot` sont LAZY : sans lui,
     * la projection en DTO lèverait une LazyInitializationException — ou, si
     * elle avait lieu dans la transaction, déclencherait deux requêtes par
     * ligne (le problème N+1, soit 41 requêtes pour une page de 20).
     *
     * La pagination reste faite par la base : `borrower` et
     * `creditScoreSnapshot` sont des associations À-UN. La limitation connue
     * d'Hibernate — pagination ramenée en mémoire — ne concerne que les
     * collections À-PLUSIEURS, absentes ici.
     *
     * Le `countQuery` est fourni explicitement et SANS les jointures : compter
     * n'a pas besoin de charger les entités liées.
     */
    @Query(
            value = "SELECT l FROM LoanRequest l JOIN FETCH l.borrower JOIN FETCH l.creditScoreSnapshot",
            countQuery = "SELECT COUNT(l) FROM LoanRequest l"
    )
    Page<LoanRequest> findAllForAdmin(Pageable pageable);

    /**
     * Même chose, filtré par statut.
     *
     * Deux méthodes distinctes plutôt qu'un unique « :status IS NULL OR ... » :
     * lier un enum à `null` empêche PostgreSQL d'inférer le type du paramètre
     * et fait échouer la requête. Deux requêtes explicites coûtent trois lignes
     * de plus et ne peuvent pas casser.
     */
    @Query(
            value = "SELECT l FROM LoanRequest l JOIN FETCH l.borrower JOIN FETCH l.creditScoreSnapshot WHERE l.status = :status",
            countQuery = "SELECT COUNT(l) FROM LoanRequest l WHERE l.status = :status"
    )
    Page<LoanRequest> findByStatusForAdmin(@Param("status") LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM LoanRequest l JOIN FETCH l.borrower JOIN FETCH l.creditScoreSnapshot WHERE l.id = :id")
    Optional<LoanRequest> findByIdForAdmin(@Param("id") Long id);

    /**
     * Volumétrie par statut : nombre de prêts, principal engagé et montant total
     * à rembourser. `COALESCE` garantit un zéro plutôt qu'un `null` sur un
     * statut sans aucun prêt.
     */
    @Query("""
            SELECT l.status, COUNT(l), COALESCE(SUM(l.requestedAmount), 0), COALESCE(SUM(l.totalRepayment), 0)
            FROM LoanRequest l
            GROUP BY l.status
            """)
    List<Object[]> summarizeByStatus();
}
