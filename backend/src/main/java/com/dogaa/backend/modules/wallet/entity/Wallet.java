package com.dogaa.backend.modules.wallet.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.WalletType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single-currency balance owned by one user. A user holds at most one wallet per
 * {@link Currency} (DOGAA.md 4.1).
 *
 * <p>The balance is split in two: {@code availableBalance} is what ordinary transactions can
 * spend, {@code lockedBalance} is money tied up in vaults (DOGAA.md 4.2) and is unspendable
 * until the vault releases it. Every movement goes through {@code WalletService} — callers
 * never write these fields directly — and the {@code @Version} column on {@link BaseEntity}
 * makes two concurrent debits on the same row fail loudly instead of interleaving.
 */
@Entity
@Table(
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_wallets_owner_currency_type",
                columnNames = {"owner_id", "currency", "type"}),
        indexes = @Index(name = "idx_wallets_owner", columnList = "owner_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {

    /** Plain id rather than a JPA relation: wallet must not own the user aggregate. */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /**
     * CURRENT for everyday money, SAVINGS for the collateral account a loan is secured against
     * (DOGAA.md 4.3). A user holds one of each per currency.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WalletType type = WalletType.CURRENT;

    @Builder.Default
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "locked_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WalletStatus status = WalletStatus.ACTIVE;

    /** Available + locked. Never negative; the service refuses any move that would break that. */
    public BigDecimal getTotalBalance() {
        return availableBalance.add(lockedBalance);
    }
}
