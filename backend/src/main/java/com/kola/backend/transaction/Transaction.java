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
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_status_created", columnList = "status, createdAt"),
        @Index(name = "idx_tx_type_currency", columnList = "type, currency"),
        @Index(name = "idx_tx_wallet_created", columnList = "wallet_id, createdAt"),
        @Index(name = "idx_tx_idempotency_key", columnList = "idempotencyKey")
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
}