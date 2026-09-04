package com.dogaa.backend.modules.user.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.Role;
import com.dogaa.backend.common.enums.UserStatus;
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
import java.time.LocalDate;

/**
 * A Dogaa account holder. The login identifier is {@link #phone} (E.164) and the secret
 * is a numeric PIN, never a password — see {@code pinHash}.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_phone", columnList = "phone", unique = true),
        @Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    /** E.164, normalised by {@code PhoneNumbers.normalize} before persisting. */
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    /** BCrypt hash of the numeric PIN. The PIN itself is never stored or logged. */
    @Column(name = "pin_hash", nullable = false, length = 100)
    private String pinHash;

    @Column(length = 160)
    private String email;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 120)
    private String address;

    @Column(length = 80)
    private String city;

    /** ISO 3166-1 alpha-2, e.g. TG, SN, CI, GH. */
    @Column(length = 2)
    private String country;

    @Builder.Default
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_tier", nullable = false, length = 10)
    private KycTier kycTier = KycTier.TIER_0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(name = "failed_pin_attempts", nullable = false)
    private int failedPinAttempts = 0;

    /** Non-null while the account is cooling down after too many wrong PINs. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }
}
