package com.dogaa.backend.modules.credit.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One rung of the leverage ladder, as saved by the back-office (BACKEND.md 7).
 *
 * <p>Versioned rather than updated in place: the screen that edits it is labelled "version
 * historisée", and for good reason — the terms a loan was granted under must stay recoverable long
 * after someone changes them. A save writes a whole new set of rows with the next version number;
 * nothing here is ever modified or deleted, and the highest version is the one in force.
 *
 * <p>The startup value comes from {@code app.credit.ladder} in the configuration file; the first
 * save is what moves ownership of the ladder from the file to the database.
 */
@Entity
@Table(name = "credit_tier_config", indexes = {
        @Index(name = "idx_credit_tier_version", columnList = "version_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditTierConfig extends BaseEntity {

    /** All rows sharing a number were saved together and are read together. */
    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    /** Ladder order, best rung first — the same order {@code CreditPolicy} reads top-down. */
    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "min_loans_repaid", nullable = false)
    private int minLoansRepaid;

    @Column(name = "min_score", nullable = false)
    private int minScore;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal leverage;

    @Column(name = "monthly_rate_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal monthlyRatePercent;

    /** Absolute ceiling for the rung. Null means the leverage alone bounds the loan. */
    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    /** Who saved this version, denormalised so the history reads without joining accounts. */
    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;
}
