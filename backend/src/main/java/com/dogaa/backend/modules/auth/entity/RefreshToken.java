package com.dogaa.backend.modules.auth.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Opaque, revocable refresh token. Only the SHA-256 hash is stored, so a database dump
 * cannot be replayed against the API. Tokens are rotated on every use.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    /** Plain id rather than a JPA relation: auth must not own the user aggregate. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
