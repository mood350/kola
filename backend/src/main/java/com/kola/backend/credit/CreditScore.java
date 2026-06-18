package com.kola.backend.credit;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot du score de crédit d'un utilisateur à un instant T.
 *
 * On ne met JAMAIS à jour un score existant : chaque recalcul crée un
 * nouveau enregistrement (immuabilité totale). Cela permet de reconstituer
 * l'historique d'évolution du score et de montrer la progression au client.
 */
@Entity
@Table(name = "credit_scores")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditScore extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Score global sur 100
    @Column(nullable = false)
    private int score;

    // Tier calculé automatiquement depuis le score
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTier tier;

    // Détail des points par critère (sérialisé en JSON)
    @Column(columnDefinition = "TEXT")
    private String breakdownJson;

    // Montant maximum de prêt autorisé à ce score
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxLoanAmount;

    // Taux mensuel applicable
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal monthlyRate;

    // Date à partir de laquelle ce score est considéré périmé (30 jours)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Indique si ce snapshot est le plus récent pour cet utilisateur
    @Column(nullable = false)
    private boolean latest = true;
}
