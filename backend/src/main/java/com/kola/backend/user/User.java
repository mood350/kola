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

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(length = 2)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private KycLevel kycLevel = KycLevel.TIER_0;

    private String password;
    private boolean enabled;
    private boolean accountLocked;

    @Builder.Default
    private int failedLoginAttempts = 0;

    // CORRECTION : Ce champ est bien distinct de "lastModifiedDate" de la classe parente
    private LocalDateTime lockedAt;

    private String lastKnownIp;
    private String lastKnownUserAgent;

    // CORRECTION 1 : Passage en LAZY pour éviter de charger les rôles à chaque requête
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;

    // CORRECTION 2 : Suppression de orphanRemoval et cascade.
    // En Fintech, ON NE SUPPRIME JAMAIS UN WALLET DE LA BASE, on le désactive (active = false)
    @OneToMany(mappedBy = "owner")
    @Builder.Default
    private List<Wallet> wallets = new ArrayList<>();

    // CORRECTION 2 : Idem pour les coffres-forts
    @OneToMany(mappedBy = "owner")
    @Builder.Default
    private List<Vault> vaults = new ArrayList<>();

    // CORRECTION 2 : Idem pour les bénéficiaires
    @OneToMany(mappedBy = "owner")
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