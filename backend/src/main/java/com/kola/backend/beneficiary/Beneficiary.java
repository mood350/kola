package com.kola.backend.beneficiary;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  Beneficiary.java                           ║
 * ║   Bénéficiaire enregistré par un utilisateur                ║
 * ║   → Permet de retrouver facilement ses proches              ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Ex : "Papa au Sénégal — Wave — +221 77 000 0000"
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "beneficiaries")
@EntityListeners(AuditingEntityListener.class)
public class Beneficiary extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Surnom du bénéficiaire (ex: "Papa", "Maman Dakar")
    @Column(nullable = false)
    private String alias;

    // Numéro de téléphone international (ex: +221770000000)
    @Column(nullable = false)
    private String phoneNumber;

    // Code pays ISO du bénéficiaire (ex: "SN", "GH", "CI")
    @Column(length = 2, nullable = false)
    private String countryCode;

    // Réseau Mobile Money du bénéficiaire
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MobileNetwork network;

    // Propriétaire de ce bénéficiaire
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}
