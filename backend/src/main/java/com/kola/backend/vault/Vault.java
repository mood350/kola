package com.kola.backend.vault;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    Vault.java                               ║
 * ║   Coffre-fort numérique (épargne programmée)                ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Permet à l'utilisateur de BLOQUER volontairement une partie
 * de ses fonds jusqu'à une date cible ou un objectif défini.
 *
 * Ex : "Scolarité 2025" → bloquer 50 000 XOF jusqu'au 01/09/2025
 *
 * RÈGLES MÉTIER :
 *  - L'argent bloqué ne peut PAS être transféré ou retiré avant unlockDate.
 *  - Le status passe automatiquement à UNLOCKED quand unlockDate est atteinte.
 *  - L'utilisateur peut ajouter des fonds à un coffre actif.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vaults")
@EntityListeners(AuditingEntityListener.class)
public class Vault extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom du coffre (ex: "Scolarité", "iPhone", "Voyage Paris")
    @Column(nullable = false)
    private String name;

    // Description / objectif du coffre
    private String purpose;

    // Montant objectif (optionnel — l'user peut ne pas fixer d'objectif)
    @Column(precision = 19, scale = 4)
    private BigDecimal targetAmount;

    // Montant actuellement bloqué dans le coffre
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    // Devise du coffre (doit correspondre à la devise du wallet source)
    @Column(nullable = false, length = 3)
    private String currency;

    // Date de déblocage prévue (null = pas de date limite)
    private LocalDate unlockDate;

    // Statut du coffre
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VaultStatus status = VaultStatus.ACTIVE;

    // Propriétaire du coffre
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
