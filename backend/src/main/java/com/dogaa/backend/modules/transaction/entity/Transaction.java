package com.dogaa.backend.modules.transaction.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
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
 * An immutable record of one money movement — the "trace transactionnelle" the spec asks
 * for at every step (DOGAA.md 4.2, 4.6). One row is written per movement and then only its
 * {@code status} changes (e.g. a chargeback flips it to {@code REVERSED}).
 *
 * <p>Wallet ids and user ids are stored plainly, not as JPA relations: this module must not
 * own the wallet or user aggregates. Either side can be null — an external cash-out has no
 * destination wallet, a cash-in has no source.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_tx_reference", columnList = "reference", unique = true),
                @Index(name = "idx_tx_sender", columnList = "sender_id"),
                @Index(name = "idx_tx_recipient", columnList = "recipient_id"),
                @Index(name = "idx_tx_source_wallet", columnList = "source_wallet_id"),
                @Index(name = "idx_tx_dest_wallet", columnList = "destination_wallet_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    /** Public, user-facing id, e.g. {@code TXN-0483920117}. */
    @Column(nullable = false, unique = true, length = 24)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** The amount that reaches the beneficiary. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Commission Dogaa keeps on top of {@link #amount}. Zero for free movement types. */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "source_wallet_id")
    private UUID sourceWalletId;

    @Column(name = "destination_wallet_id")
    private UUID destinationWalletId;

    @Column(name = "sender_id")
    private UUID senderId;

    /** Null when the money leaves the Dogaa ecosystem (external number, cash-out). */
    @Column(name = "recipient_id")
    private UUID recipientId;

    /** External number, merchant code or QR payload — whatever identifies the other side. */
    @Column(length = 64)
    private String counterparty;

    @Column(length = 140)
    private String description;

    /** Set when {@link #status} is {@code FAILED}. */
    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** What the sender actually parts with: {@link #amount} + {@link #fee}. */
    public BigDecimal getTotalDebited() {
        return amount.add(fee);
    }
}
