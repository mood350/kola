package com.dogaa.backend.modules.vault.entity;

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
import java.time.LocalDate;
import java.util.UUID;

/**
 * A savings goal — a "coffre-fort" (DOGAA.md 4.2). Money paid into a vault leaves the
 * funding wallet's available balance and sits in its locked balance, so ordinary
 * transactions can no longer spend it.
 *
 * <p>{@link #balance} is this vault's slice of {@code wallet.lockedBalance}; the sum of a
 * user's active vault balances in a currency equals the locked balance of their wallet in
 * that currency. Every move goes through {@code VaultService}, which drives
 * {@code WalletService.lock/unlock}.
 */
@Entity
@Table(name = "vaults", indexes = {
        @Index(name = "idx_vaults_owner", columnList = "owner_id"),
        @Index(name = "idx_vaults_wallet", columnList = "wallet_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vault extends BaseEntity {

    /** Plain id rather than a JPA relation: vault must not own the user aggregate. */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** The owner's wallet in {@link #currency} that funds this vault. */
    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** How much is currently locked in this vault. */
    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Savings target. Null means "no fixed goal". */
    @Column(name = "target_amount", precision = 19, scale = 2)
    private BigDecimal targetAmount;

    /** Optional deadline the user set for the goal. */
    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(length = 160)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VaultStatus status = VaultStatus.ACTIVE;

    public boolean isGoalReached() {
        return targetAmount != null && balance.compareTo(targetAmount) >= 0;
    }
}
