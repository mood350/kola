package com.kola.backend.modules.scoring.entity;

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

import java.util.UUID;

/**
 * One nightly evaluation, kept rather than overwritten.
 *
 * <p>Both the raw and the smoothed values are stored: the raw one is what today's behaviour
 * deserves, the smoothed one is what the user is granted. Keeping the history and the per-axis
 * breakdown is what makes a refusal explainable months later, which matters as much to a regulator
 * as it does to the user.
 */
@Entity
@Table(name = "credit_scores",
        indexes = @Index(name = "idx_credit_scores_user_created", columnList = "user_id, created_at"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScore extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The published score: today's raw value blended with yesterday's. */
    @Column(name = "score_value", nullable = false)
    private int scoreValue;

    /** What the observation window alone would give, before smoothing. */
    @Column(name = "raw_score_value", nullable = false)
    private int rawScoreValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_tier", nullable = false, length = 10)
    private KycTier kycTier;

    @Column(name = "savings_discipline_points", nullable = false)
    private int savingsDisciplinePoints;

    @Column(name = "financial_stability_points", nullable = false)
    private int financialStabilityPoints;

    @Column(name = "inflow_regularity_points", nullable = false)
    private int inflowRegularityPoints;

    @Column(name = "usage_intensity_points", nullable = false)
    private int usageIntensityPoints;

    @Column(name = "credit_history_points", nullable = false)
    private int creditHistoryPoints;

    @Column(name = "window_days", nullable = false)
    private int windowDays;
}
