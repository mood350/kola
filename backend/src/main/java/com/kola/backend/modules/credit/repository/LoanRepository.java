package com.kola.backend.modules.credit.repository;

import com.kola.backend.common.enums.LoanStatus;
import com.kola.backend.modules.credit.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Loan> findFirstByUserIdAndStatusIn(UUID userId, Collection<LoanStatus> statuses);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<LoanStatus> statuses);

    List<Loan> findByStatusIn(Collection<LoanStatus> statuses);

    List<Loan> findByStatusInAndDueAtBefore(Collection<LoanStatus> statuses, Instant cutoff);

    long countByUserIdAndStatus(UUID userId, LoanStatus status);

    long countByStatus(LoanStatus status);

    /** What the whole book still owes: the "encours de prets" metric. */
    @Query("""
            select coalesce(sum(l.principal + l.interestAmount + l.penaltyAmount - l.amountRepaid), 0)
            from Loan l where l.status in :statuses
            """)
    BigDecimal sumOutstanding(@Param("statuses") Collection<LoanStatus> statuses);

    /** Interest actually earned on the loans in these states — the credit half of revenue. */
    @Query("""
            select coalesce(sum(l.interestAmount + l.penaltyAmount), 0)
            from Loan l where l.status in :statuses
            """)
    BigDecimal sumInterestEarned(@Param("statuses") Collection<LoanStatus> statuses);

    /** Loans past their due date, oldest first: the recovery queue. */
    List<Loan> findByStatusInAndDueAtBeforeOrderByDueAtAsc(
            Collection<LoanStatus> statuses, Instant cutoff);
}
