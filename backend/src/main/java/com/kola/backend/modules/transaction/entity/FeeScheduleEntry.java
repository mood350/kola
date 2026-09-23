package com.kola.backend.modules.transaction.entity;

import com.kola.backend.common.audit.BaseEntity;
import com.kola.backend.common.enums.KycTier;
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

/**
 * The commission charged to one KYC tier (KOLA.md 4.1, BACKEND.md 10).
 *
 * <p>Versioned like the lending ladder, and for the same reason: a customer disputing a fee months
 * later is asking what the rate was <em>then</em>. Rows are appended, never edited, and the highest
 * version is the one in force.
 *
 * <p>Rates are stored per tier rather than as a base rate times a rebate. The back-office edits the
 * grid the customer actually pays, so that is what is persisted — deriving it from a multiplier
 * would mean the saved number and the charged number could drift apart.
 */
@Entity
@Table(name = "fee_schedule", indexes = {
        @Index(name = "idx_fee_schedule_version", columnList = "version_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeScheduleEntry extends BaseEntity {

    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KycTier tier;

    /** Percent of the transferred amount, e.g. 1.50. */
    @Column(name = "p2p_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal p2pPercent;

    @Column(name = "merchant_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal merchantPercent;

    @Column(name = "cash_out_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal cashOutPercent;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;
}
