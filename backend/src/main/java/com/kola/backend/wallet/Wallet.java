package com.kola.backend.wallet;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    Wallet.java                              ║
 * ║   Portefeuille numérique de l'utilisateur                   ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Chaque wallet est lié à une devise (ex: XOF pour le Togo/Sénégal).
 * Un utilisateur peut avoir plusieurs wallets (multi-devises).
 *
 * IMPORTANT : On utilise BigDecimal pour les montants financiers,
 * JAMAIS double ou float (risque d'erreurs d'arrondi).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallets")
@EntityListeners(AuditingEntityListener.class)
public class Wallet extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Code devise ISO 4217 (ex: "XOF", "GHS", "NGN", "USD")
    @Column(nullable = false, length = 3)
    private String currency;

    // Solde disponible (BigDecimal obligatoire pour la finance)
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    // Solde bloqué (somme des coffres-forts actifs de cette devise)
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    // Wallet actif ou suspendu (gel de compte par admin)
    @Builder.Default
    private boolean active = true;

    // Propriétaire du wallet
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
