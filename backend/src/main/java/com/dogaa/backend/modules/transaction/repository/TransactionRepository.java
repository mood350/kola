package com.dogaa.backend.modules.transaction.repository;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
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
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    /** Everything the user was on either side of, newest first. */
    @Query("""
            select t from Transaction t
            where t.senderId = :userId or t.recipientId = :userId
            order by t.createdAt desc
            """)
    Page<Transaction> findForUser(@Param("userId") UUID userId, Pageable pageable);

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
}
