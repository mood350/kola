package com.dogaa.backend.modules.auth.entity;

import com.dogaa.backend.common.audit.BaseEntity;
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
 * One OTP challenge, and the proof produced once it is answered correctly.
 *
 * <p>The row carries both halves on purpose: the verification token only ever exists on a row
 * whose code was verified, so "holds a valid token" and "answered the OTP" cannot come apart.
 * Registration reads the phone number from this row, never from the request body.
 */
@Entity
@Table(name = "otp_codes", indexes = {
        @Index(name = "idx_otp_codes_phone_purpose", columnList = "phone, purpose"),
        @Index(name = "idx_otp_codes_token", columnList = "token_hash", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpPurpose purpose;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OtpChannel channel = OtpChannel.SMS;

    /**
     * Where the code was actually sent: the phone number for SMS, the address for email.
     * The row stays keyed by {@code phone} because that is what identifies the account.
     */
    @Column(nullable = false, length = 160)
    private String destination;

    /** BCrypt hash: a 6-digit code is only a million guesses, so it needs a slow hash. */
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /** SHA-256 of the single-use token handed out after a correct code. */
    @Column(name = "token_hash", unique = true, length = 64)
    private String tokenHash;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    /** Set when the challenge is spent or abandoned; a consumed row can never be reused. */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    public boolean isPending() {
        return consumedAt == null && verifiedAt == null && expiresAt.isAfter(Instant.now());
    }

    public boolean isTokenUsable() {
        return consumedAt == null
                && verifiedAt != null
                && tokenExpiresAt != null
                && tokenExpiresAt.isAfter(Instant.now());
    }
}
