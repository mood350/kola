package com.kola.backend.user;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.role.Role;
import com.kola.backend.utils.Listeners;
import com.kola.backend.vault.Vault;
import com.kola.backend.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_user")
@EntityListeners(AuditingEntityListener.class)
public class User extends Listeners implements Principal, UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    // Clé de voûte de l'application : numéro de téléphone (ex: +22890000000)
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    // Code pays ISO 3166-1 alpha-2 (ex: "TG", "SN", "CI", "GH")
    @Column(length = 2)
    private String countryCode;

    // Niveau de vérification KYC (débloque les fonctionnalités progressivement)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KycLevel kycLevel = KycLevel.TIER_0;

    private String password;
    private boolean enabled;
    private boolean accountLocked;

    // Compteur de tentatives de connexion échouées consécutives.
    // Remis à zéro à chaque connexion réussie.
    @Builder.Default
    private int failedLoginAttempts = 0;

    // Horodatage du dernier verrouillage (utile pour un déverrouillage automatique après X minutes)
    private LocalDateTime lockedAt;

    // Pour la détection d'un nouvel appareil/IP (comme Google)
    private String lastKnownIp;
    private String lastKnownUserAgent;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;

    // Portefeuille(s) de l'utilisateur (XOF, USD, etc.)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Wallet> wallets = new ArrayList<>();

    // Coffres-forts (épargne programmée)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vault> vaults = new ArrayList<>();

    // Bénéficiaires enregistrés (proches fréquemment payés)
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Beneficiary> beneficiaries = new ArrayList<>();

    public String fullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String getName() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}

