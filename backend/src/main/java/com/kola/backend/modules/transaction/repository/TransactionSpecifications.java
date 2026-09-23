package com.kola.backend.modules.transaction.repository;

import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filters for the transaction history, built as a criteria query rather than one JPQL string with
 * {@code :param is null or ...} in it.
 *
 * <p>That pattern works on H2 and fails on PostgreSQL: with no value bound, the driver has nothing
 * to infer a type from and the server answers "could not determine data type of parameter". The
 * test suite runs on H2, so the query passed every test and returned 500 in production. Building
 * the predicate list means an absent filter contributes no parameter at all, which is both correct
 * everywhere and a cheaper query.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    /** Everything the user was on either side of, narrowed by whichever filters are present. */
    public static Specification<Transaction> forUser(UUID userId,
                                                     TransactionType type,
                                                     TransactionStatus status,
                                                     Instant from,
                                                     Instant to) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.or(
                    builder.equal(root.get("senderId"), userId),
                    builder.equal(root.get("recipientId"), userId)));

            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
