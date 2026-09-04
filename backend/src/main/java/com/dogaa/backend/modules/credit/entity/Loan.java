package com.dogaa.backend.modules.credit.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.LoanStatus;
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
 * A loan secured against the borrower's savings account (DOGAA.md 4.3).
 *
 * <p>Everything needed to explain the deal is frozen on the row at grant time: the collateral it
 * was secured against, the leverage and rate that applied, and the score that earned them. Terms
 * are configuration and configuration changes; a contract must not.
 */
@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loans_user", columnList = "user_id"),
        @Index(name = "idx_loans_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /** The savings wallet whose balance is frozen for the life of the loan. */
    @Column(name = "savings_wallet_id", nullable = false)
    private UUID savingsWalletId;

    /** The current wallet the money was paid onto and will be pulled back from. */
    @Column(name = "current_wallet_id", nullable = false)
    private UUID currentWalletId;

    /** Savings balance at grant time: what secures this loan. */
    @Column(name = "collateral_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal collateralAmount;

    /** Multiple of the collateral that was lent. At 1.0 the loan cannot lose money. */
    @Column(name = "leverage_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal leverageRatio;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principal;

    @Column(name = "monthly_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal monthlyRatePercent;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Builder.Default
    @Column(name = "penalty_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "amount_repaid", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountRepaid = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private LoanStatus status = LoanStatus.ACTIVE;

    @Column(name = "score_at_grant", nullable = false)
    private int scoreAtGrant;

    @Column(name = "disbursed_at", nullable = false)
    private Instant disbursedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    /** What is left owing after the collateral was seized, if anything. */
    @Builder.Default
    @Column(name = "shortfall_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal shortfallAmount = BigDecimal.ZERO;

    public BigDecimal getTotalDue() {
        return principal.add(interestAmount).add(penaltyAmount);
    }

    public BigDecimal getOutstanding() {
        return getTotalDue().subtract(amountRepaid).max(BigDecimal.ZERO);
    }

    public boolean isSettledOnTime() {
        return status == LoanStatus.REPAID && settledAt != null && !settledAt.isAfter(dueAt);
    }
}
