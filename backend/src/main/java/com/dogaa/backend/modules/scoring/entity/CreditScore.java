package com.dogaa.backend.modules.scoring.entity;

import com.dogaa.backend.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credit_scores")
@Getter
@Setter
@NoArgsConstructor
public class CreditScore extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "score_value", nullable = false)
    private int scoreValue;

    @Column(name = "kyc_eligible", nullable = false)
    private boolean kycEligible;

    @Column(name = "deposit_regularity_points", nullable = false)
    private int depositRegularityPoints;

    @Column(name = "savings_discipline_points", nullable = false)
    private int savingsDisciplinePoints;

    @Column(name = "transaction_diversity_points", nullable = false)
    private int transactionDiversityPoints;

    @Column(name = "balance_stability_points", nullable = false)
    private int balanceStabilityPoints;

    @Column(name = "scheduled_reliability_points", nullable = false)
    private int scheduledReliabilityPoints;
}
