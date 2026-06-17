package com.kola.backend.transaction;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import com.kola.backend.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  Transaction.java                           ║
 * ║   Trace inaltérable de tout mouvement financier Kola        ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * RÈGLE D'OR : On ne modifie JAMAIS une transaction.
 * En cas d'annulation, on crée une nouvelle transaction REFUNDED.
 *
 * EXEMPLE pour un transfert Togo → Sénégal :
 *  - Transaction 1 (TRANSFER_OUT) : Sender = user togolais, amount = 50 000 XOF
 *  - Transaction 2 (FEE)          : amount = 500 XOF (frais Kola)
 *  - Transaction 3 (TRANSFER_IN)  : Receiver = numéro sénégalais, amount = 49 500 XOF
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
public class Transaction extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence unique générée pour chaque transaction (ex: KLA-2025-XXXXXX)
    @Column(unique = true, nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    // Montant de la transaction (AVANT déduction des frais)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // Frais Kola prélevés sur cette transaction
    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    // Devise de la transaction (côté émetteur)
    @Column(nullable = false, length = 3)
    private String currency;

    // ─── Transferts Cross-Border ───────────────────────────────────
    // Devise du destinataire (peut être différente de la devise source)
    @Column(length = 3)
    private String receiverCurrency;

    // Taux de change appliqué au moment de la transaction
    // (conservé pour l'audit — le taux change chaque jour)
    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    // ─── Relations ────────────────────────────────────────────────

    // Wallet source de la transaction (peut être null pour TRANSFER_IN)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // Utilisateur émetteur (null si transaction entrante externe)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    // Numéro de téléphone du destinataire (peut être externe à Kola)
    private String receiverPhoneNumber;

    // Code pays du destinataire
    @Column(length = 2)
    private String receiverCountryCode;

    // Utilisateur destinataire (null si destinataire externe à Kola)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    // Description libre ou motif du transfert
    private String description;

    // Référence de transaction externe (ID retourné par l'opérateur Mobile Money)
    private String externalReference;
}
