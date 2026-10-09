package com.kola.backend.modules.admin.entity;

import com.kola.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A member of back-office staff.
 *
 * <p>Deliberately not a {@code User}. Staff are a different population with different credentials
 * — an email and a password, not a phone and a PIN — different lifecycles and different risks.
 * Folding them into the customer table would mean every wallet query carries staff rows, and every
 * staff permission change risks touching a customer.
 */
@Entity
@Table(name = "admin_accounts",
        indexes = @Index(name = "idx_admin_accounts_email", columnList = "email", unique = true))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminRole role;

    /** Free text shown on the profile screen, e.g. "KYC, litiges, chargebacks (2e validation)". */
    @Column(length = 200)
    private String scope;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** "Sena Amétépé" becomes "SA": the admin list shows initials in an avatar. */
    public String getInitials() {
        String[] parts = name.trim().split("\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
