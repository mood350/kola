package com.kola.backend.transaction;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import com.kola.backend.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// OPTIMISATION : Ajout d'index pour accélérer les requêtes du Dashboard Admin
// et les lectures par expéditeur/destinataire (LAB-FT, scoring, plafonds KYC).
// Pas d'index sur idempotencyKey : la contrainte unique en crée déjà un.
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_tx_type_currency", columnList = "type, currency"),
        @Index(name = "idx_tx_wallet_created", columnList = "wallet_id, createdAt"),
        @Index(name = "idx_tx_sender_created", columnList = "sender_id, createdAt"),
        @Index(name = "idx_tx_receiver_created", columnList = "receiver_id, createdAt")
})
@EntityListeners(AuditingEntityListener.class)
public class Transaction extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 3)
    private String receiverCurrency;

    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    private String receiverPhoneNumber;

    @Column(length = 2)
    private String receiverCountryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String description;

    private String externalReference;

    @Column(unique = true)
    private String idempotencyKey;

    /**
     * Prestataire d'encaissement, quand l'argent est entré par un tiers.
     *
     * Nul pour tout ce qui reste interne à Kola (virements entre wallets,
     * mouvements de coffre, frais) : ces écritures n'ont pas de contrepartie
     * externe, et leur donner un prestataire fictif fausserait tout
     * rapprochement.
     */
    @Column(length = 20)
    private String provider;

    /**
     * Identifiant de l'opération CHEZ le prestataire.
     *
     * C'est la seule clé dont dispose un webhook pour retrouver cette écriture :
     * la notification ne connaît pas les identifiants de Kola. Unique, parce
     * qu'une même opération externe ne doit jamais pouvoir créditer deux
     * écritures — c'est ce qui rend le rejeu inoffensif au niveau de la base,
     * et pas seulement au niveau du code.
     */
    @Column(unique = true, length = 64)
    private String providerTransactionId;
}