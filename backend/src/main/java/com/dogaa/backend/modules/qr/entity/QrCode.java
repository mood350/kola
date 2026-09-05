package com.dogaa.backend.modules.qr.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A scannable code standing for "pay this account".
 *
 * <p><b>What it contains is a random reference, never the phone number.</b> A QR gets photographed,
 * printed and forwarded; encoding the number would turn every shared code into a permanent handout
 * of the owner's phone number, and there would be no way to take it back. A reference can be
 * revoked, and it resolves only for a signed-in caller.
 *
 * <p>The owner is a plain {@code ownerId}: this module reads accounts through the user and
 * transaction services, it does not own them.
 */
@Entity
@Table(name = "qr_codes", indexes = {
        @Index(name = "idx_qr_code_value", columnList = "code", unique = true),
        @Index(name = "idx_qr_code_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
public class QrCode extends BaseEntity {

    /** The random reference carried by the image. Unique, unguessable, revocable. */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private QrCodeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QrCodeStatus status = QrCodeStatus.ACTIVE;

    /** Null on a STATIC code: the payer decides. Set and binding on a PAYMENT_REQUEST. */
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private Currency currency;

    /** What the payer sees before confirming — "Table 4", "Facture 2043". */
    @Column(length = 140)
    private String label;

    /** Null means it never expires, which is only ever true of a STATIC code. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Who paid it. Kept for the owner's own history, never shown to another payer. */
    @Column(name = "paid_by_user_id")
    private UUID paidByUserId;

    /** The transaction the payment produced, so a request can be traced to its money. */
    @Column(name = "transaction_reference", length = 40)
    private String transactionReference;

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    /** Scannable right now: active, and not past its expiry. */
    public boolean isPayable(Instant now) {
        return status == QrCodeStatus.ACTIVE && !isExpired(now);
    }
}
