package com.dogaa.backend.modules.admin.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A contested transaction (DOGAA.md 4.5, BACKEND.md 9).
 *
 * <p>Keyed by the transaction's public reference, which is what the console shows and what every
 * endpoint addresses it by. The transaction id is kept alongside so a chargeback can find the two
 * wallets without parsing anything.
 *
 * <p>{@code validationsRequired} is stored per dispute rather than hard-coded: two is the rule
 * today, but the count belongs to the claim it was opened under — raising the bar later must not
 * silently reopen claims that already met the old one.
 */
@Entity
@Table(name = "disputes", indexes = {
        @Index(name = "idx_dispute_reference", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_dispute_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute extends BaseEntity {

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 24)
    private String transactionReference;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeTag tag;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** One line describing the claim, e.g. "Paiement marchand contesté · Boutique Sika". */
    @Column(nullable = false, length = 160)
    private String title;

    /** Who filed it — the account whose money is in question. */
    @Column(name = "claimant_id", nullable = false)
    private UUID claimantId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Builder.Default
    @Column(name = "validations_required", nullable = false)
    private int validationsRequired = 2;

    /** Set when the claim reaches a decision, so the console can say what happened last. */
    @Column(name = "last_validation_note", length = 200)
    private String lastValidationNote;
}
