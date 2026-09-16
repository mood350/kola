package com.kola.backend.modules.scoring.entity;

import com.kola.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One row per user per day, written by the nightly job.
 *
 * <p>Without this the "average balance" and "volatility" signals are uncomputable: a wallet only
 * knows what it holds right now, so a user who is swept to zero every week looks identical to one
 * who never dips. One small row a day buys the whole of axis B.
 */
@Entity
@Table(name = "daily_balance_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_balance_user_day", columnNames = {"user_id", "snapshot_date"}),
        indexes = @Index(name = "idx_daily_balance_user_date", columnList = "user_id, snapshot_date"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBalanceSnapshot extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /** Everything the user holds that day, current and savings, available and locked. */
    @Column(name = "total_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "savings_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal savingsBalance;
}
