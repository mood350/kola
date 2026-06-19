package com.kola.backend.scheduler;

import com.kola.backend.user.User;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Représente un virement programmé (ex: épargne automatique le 1er du mois).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scheduled_transfers")
@EntityListeners(AuditingEntityListener.class)
public class ScheduledTransfer extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "MONTHLY" pour chaque mois, "WEEKLY" pour chaque semaine
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    // Jour d'exécution (ex: 1 pour le 1er du mois, 15 pour le 15)
    @Column(nullable = false)
    private int executionDay;

    // Montant à transférer
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // Devise
    @Column(nullable = false, length = 3)
    private String currency;

    // Motif (ex: "Épargne mensuelle voiture")
    private String description;

    // Statut
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScheduledStatus status = ScheduledStatus.ACTIVE;

    // Dernière date d'exécution réussie
    private LocalDateTime lastExecutedAt;

    // Prochaine date d'exécution prévue
    private LocalDateTime nextExecutionDate;

    // Propriétaire
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    // Wallet source
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Wallet destination (ex: un sous-compte épargne, ou un coffre-fort)
    // Ici on suppose que c'est vers un Vault, mais tu peux adapter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_vault_id")
    private com.kola.backend.vault.Vault targetVault;

    public enum Frequency {
        MONTHLY, WEEKLY
    }

    public enum ScheduledStatus {
        ACTIVE, PAUSED, FAILED_PERMANENTLY
    }
}