package com.kola.backend.modules.transaction.entity;

import com.kola.backend.common.audit.BaseEntity;
import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
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
 * for at every step (KOLA.md 4.2, 4.6). One row is written per movement and then only its
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

    /** Commission Kola keeps on top of {@link #amount}. Zero for free movement types. */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "source_wallet_id")
    private UUID sourceWalletId;

    @Column(name = "destination_wallet_id")
    private UUID destinationWalletId;

    @Column(name = "sender_id")
    private UUID senderId;

    /** Null when the money leaves the Kola ecosystem (external number, cash-out). */
    @Column(name = "recipient_id")
    private UUID recipientId;

    /** External number, merchant code or QR payload — whatever identifies the other side. */
    @Column(length = 64)
    private String counterparty;

    /**
     * The other party's name as it stood when the money moved.
     *
     * <p>A snapshot, not a live join: a history line must keep saying who was paid even after that
     * person renames their account, and a name resolved today would rewrite what the user
     * remembers confirming. Null when the number belongs to nobody on Kola.
     */
    @Column(name = "counterparty_name", length = 120)
    private String counterpartyName;

    @Column(length = 140)
    private String description;

    /** Set when {@link #status} is {@code FAILED}. */
    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    /**
     * Caller-supplied key making a retry safe (see {@code TransactionService.executeIdempotent}).
     *
     * <p>Unique, and that uniqueness is the mechanism: the database refuses the second insert, so
     * a retried request cannot pay twice even if two servers process it at the same instant. A
     * check-then-insert in application code would leave a window between the two.
     *
     * <p>Null for movements nobody can replay — an admin chargeback, a vault movement recorded
     * after the money already moved.
     */
    // Not updatable=false: the key is stamped by executeIdempotent just after the movement has
    // built and saved its row, so Hibernate has to include the column in that UPDATE. Marking it
    // non-updatable silently dropped the stamp, and every retry then paid again.
    @Column(name = "idempotency_key", unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** What the sender actually parts with: {@link #amount} + {@link #fee}. */
    public BigDecimal getTotalDebited() {
        return amount.add(fee);
    }
}
