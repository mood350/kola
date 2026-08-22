package com.kola.backend.merchant;

import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * Marchand pouvant recevoir des paiements via QR code (scan du merchantCode).
 * Pas de compte utilisateur associé dans cette version — un marchand n'est
 * qu'un point de réception, sans login ni historique consulté depuis l'app
 * mobile. Solde géré directement ici plutôt que via l'entité Wallet : Wallet
 * exige un owner (User) non-nul (invariant IDOR délibéré ailleurs), qu'un
 * marchand sans compte utilisateur n'a pas de raison de contourner.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "merchants")
@EntityListeners(AuditingEntityListener.class)
public class Merchant extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(nullable = false, unique = true)
    private String merchantCode;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "XOF";
}
