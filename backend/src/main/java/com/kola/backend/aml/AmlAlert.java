package com.kola.backend.aml;

import com.kola.backend.transaction.Transaction;
import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "aml_alerts", indexes = {
        @Index(name = "idx_aml_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_aml_risk_level", columnList = "riskLevel"),
        @Index(name = "idx_aml_user", columnList = "user_id"),
        @Index(name = "idx_aml_transaction", columnList = "transaction_id")
})
@EntityListeners(AuditingEntityListener.class)
public class AmlAlert extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Transaction déclenchante. Null si l'alerte porte sur un motif global. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmlRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AmlAlertStatus status = AmlAlertStatus.OPEN;

    /** Détail JSON des règles déclenchées (libellé, poids, explication). */
    @Column(columnDefinition = "TEXT")
    private String triggeredRulesJson;

    @Column(length = 1000)
    private String reviewNotes;

    private String reviewedBy;
}
