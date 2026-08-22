package com.kola.backend.vault;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import com.kola.backend.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vaults", indexes = {
        @Index(name = "idx_vault_owner_status", columnList = "owner_id, status"),
        @Index(name = "idx_vault_wallet", columnList = "wallet_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Vault extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String purpose;

    @Column(precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;

    /**
     * Même rôle que Wallet.version (cf. commentaire détaillé là-bas).
     *
     * Particulièrement utile ici : VaultService lit le coffre SANS verrou
     * avant de libérer les fonds, si bien que deux déblocages simultanés
     * peuvent tous deux lire le même currentAmount et le créditer deux fois au
     * portefeuille. Ce compteur transforme cette double libération silencieuse
     * en échec explicite.
     */
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;

    @Column(nullable = false, length = 3)
    private String currency;

    private LocalDate unlockDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VaultStatus status = VaultStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
}