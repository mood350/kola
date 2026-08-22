package com.kola.backend.wallet;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
// L'unicité (propriétaire, devise) est l'invariant que WalletService.createWallet
// vérifiait par un existsBy... : deux créations concurrentes le franchissaient
// toutes les deux. La contrainte l'impose atomiquement, et son index sert les
// recherches par propriétaire — dont le verrou pessimiste des mouvements d'argent.
@Table(name = "wallets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_owner_currency", columnNames = {"owner_id", "currency"})
})
@EntityListeners(AuditingEntityListener.class)
public class Wallet extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Builder.Default
    private boolean active = true;

    /**
     * Verrouillage optimiste : filet de sécurité derrière les verrous
     * pessimiste posés sur les chemins monétaires.
     *
     * Les verrous explicites (findOwnedWalletForUpdateOrThrow,
     * lockAllForUpdate) protègent les chemins qu'on a identifiés ; ce compteur
     * protège de ceux qu'on oubliera. Toute écriture concurrente sur un
     * portefeuille chargé sans verrou échoue désormais bruyamment (409) au
     * lieu d'écraser silencieusement le solde de l'autre transaction — c'est
     * exactement le scénario de lost update qui affectait le déboursement de
     * prêt et les virements programmés.
     *
     * `columnDefinition` porte un DEFAULT 0 : avec ddl-auto=update, la colonne
     * est ajoutée à une table déjà peuplée et les lignes existantes doivent
     * être initialisées, sinon leur version reste NULL et la première mise à
     * jour échoue.
     */
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}