package com.dogaa.backend.modules.credit.repository;

import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.modules.credit.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
