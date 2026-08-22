package com.kola.backend.beneficiary;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
// Même raisonnement que Wallet : la contrainte remplace le existsBy... de
// BeneficiaryService, et indexe owner_id au passage.
@Table(name = "beneficiaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_benef_owner_phone_network",
                columnNames = {"owner_id", "phone_number", "network"})
})
@EntityListeners(AuditingEntityListener.class)
public class Beneficiary extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(length = 2, nullable = false)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MobileNetwork network;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
}