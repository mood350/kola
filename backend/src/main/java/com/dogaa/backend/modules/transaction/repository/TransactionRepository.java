package com.dogaa.backend.modules.transaction.repository;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.modules.transaction.dto.TransactionAggregate;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    /**
     * Everything the user was on either side of, newest first, optionally narrowed by
     * type, status and/or a creation-date window — any filter left {@code null} is
     * ignored. Includes {@code FAILED} traces (scheduler misfires included), so this is
     * the one place a user's transactional activity is fully visible.
     */
    @Query("""
            select t from Transaction t
            where (t.senderId = :userId or t.recipientId = :userId)
              and (:type is null or t.type = :type)
              and (:status is null or t.status = :status)
              and (:from is null or t.createdAt >= :from)
              and (:to is null or t.createdAt <= :to)
            order by t.createdAt desc
            """)
    Page<Transaction> findForUser(@Param("userId") UUID userId,
                                  @Param("type") TransactionType type,
                                  @Param("status") TransactionStatus status,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  Pageable pageable);

    /**
     * Sum of what the user has already sent out today in one currency — the running total
     * the KYC daily limit is checked against (DOGAA.md 4.4).
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

    /**
     * Transaction count, total amount moved and total fees collected, one row per
     * currency (DOGAA.md 4.5: "Volume de transactions"). Consumed by dogaa-admin
     * through {@code TransactionService}, never directly.
     */
    @Query("""
            select new com.dogaa.backend.modules.transaction.dto.TransactionAggregate(
                t.currency, count(t), coalesce(sum(t.amount), 0), coalesce(sum(t.fee), 0))
            from Transaction t
            where t.status = :status
            group by t.currency
            """)
    List<TransactionAggregate> aggregateByCurrency(@Param("status") TransactionStatus status);
}
