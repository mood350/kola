package com.kola.backend.modules.transaction.repository;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.dto.FeeAggregate;
import com.kola.backend.modules.transaction.dto.TransactionAggregate;
import com.kola.backend.modules.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface TransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReference(String reference);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Sum of what the user has already sent out today in one currency: the running total
     * the KYC daily limit is checked against (KOLA.md 4.4).
     */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.senderId = :userId
              and t.currency = :currency
              and t.status = :status
              and t.type in :types
              and t.createdAt >= :since
            """)
    BigDecimal sumSentSince(@Param("userId") UUID userId,
                            @Param("currency") Currency currency,
                            @Param("status") TransactionStatus status,
                            @Param("types") Collection<TransactionType> types,
                            @Param("since") Instant since);

    /** Total moved since an instant, all currencies pooled: the 24-hour volume metric. */
    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.status = :status and t.createdAt >= :since
            """)
    BigDecimal sumAmountSince(@Param("status") TransactionStatus status,
                              @Param("since") Instant since);

    /** Every completed amount since an instant, with its timestamp: raw material for the chart. */
    @Query("""
            select t.createdAt, t.amount from Transaction t
            where t.status = :status and t.createdAt >= :since
            order by t.createdAt asc
            """)
    List<Object[]> completedAmountsSince(@Param("status") TransactionStatus status,
                                         @Param("since") Instant since);

    /**
     * Transaction count, total amount moved and total fees collected, one row per
     * currency (KOLA.md 4.5: "Volume de transactions"). Consumed by kola-admin
     * through {@code TransactionService}, never directly.
     */
    @Query("""
            select new com.kola.backend.modules.transaction.dto.TransactionAggregate(
                t.currency, count(t), coalesce(sum(t.amount), 0), coalesce(sum(t.fee), 0))
            from Transaction t
            where t.status = :status
            group by t.currency
            """)
    List<TransactionAggregate> aggregateByCurrency(@Param("status") TransactionStatus status);

    /**
     * Fees collected per movement type (KOLA.md 5.3.A), for the revenue breakdown. Types that
     * never charge a commission simply produce no row.
     */
    @Query("""
            select new com.kola.backend.modules.transaction.dto.FeeAggregate(
                t.type, coalesce(sum(t.fee), 0))
            from Transaction t
            where t.status = :status
            group by t.type
            having coalesce(sum(t.fee), 0) > 0
            """)
    List<FeeAggregate> aggregateFeesByType(@Param("status") TransactionStatus status);
}
