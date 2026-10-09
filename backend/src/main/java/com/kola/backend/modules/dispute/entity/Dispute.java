package com.kola.backend.modules.dispute.entity;

import com.kola.backend.common.audit.BaseEntity;
import com.kola.backend.common.enums.Currency;
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
import java.util.UUID;

/**
 * A contested transaction (KOLA.md 4.5, BACKEND.md 9).
 *
 * <p>Keyed by the transaction reference because that is what a customer quotes and what the
 * back-office searches on. Resolving one moves real money between two customers on nothing but an
 * internal decision, which is why {@link DisputeValidation} exists.
 */
@Entity
@Table(name = "disputes", indexes = {
        @Index(name = "idx_disputes_reference", columnList = "transaction_reference", unique = true),
        @Index(name = "idx_disputes_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute extends BaseEntity {

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 40)
    private String transactionReference;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    /** Who reported it — normally the debited party. */
    @Column(name = "reported_by", nullable = false)
    private UUID reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeTag tag;

    @Column(nullable = false, length = 160)
    private String title;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** Frozen at grant time so the panel keeps showing the requirement that actually applied. */
    @Column(name = "validations_required", nullable = false)
    private int validationsRequired;

    @Column(name = "last_validation_note", length = 300)
    private String lastValidationNote;

    /**
     * What could not be recovered from the party who received the money, because they had already
     * spent it. The victim is made whole regardless; this is what the decision cost the platform.
     */
    @Builder.Default
    @Column(name = "shortfall_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shortfallAmount = BigDecimal.ZERO;
}
