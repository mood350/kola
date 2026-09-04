package com.dogaa.backend.modules.auth.service;

import com.dogaa.backend.config.JwtProperties;
import com.dogaa.backend.exception.UnauthorizedException;
import com.dogaa.backend.modules.auth.entity.RefreshToken;
import com.dogaa.backend.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Issues, rotates and revokes opaque refresh tokens. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /** @return the clear-text token; only its hash is persisted. */
    @Transactional
    public String issue(UUID userId, String userAgent, String ipAddress) {
        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(token))
                .expiresAt(Instant.now().plus(jwtProperties.getRefreshTokenTtl()))
                .userAgent(truncate(userAgent, 255))
                .ipAddress(truncate(ipAddress, 45))
                .build());
        return token;
    }

    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN",
                        "Refresh token is invalid"));
        if (!stored.isActive()) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN",
                    "Refresh token has expired or been revoked");
        }
        return stored;
    }

    /**
     * Rotation: the presented token is burned and a fresh one returned. Re-use of an already
     * rotated token is therefore detectable — it simply fails to verify.
     */
    @Transactional
    public String rotate(RefreshToken current, String userAgent, String ipAddress) {
        current.setRevokedAt(Instant.now());
        refreshTokenRepository.save(current);
        return issue(current.getUserId(), userAgent, ipAddress);
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenHash(hash(token)).ifPresent(stored -> {
            if (stored.getRevokedAt() == null) {
                stored.setRevokedAt(Instant.now());
                refreshTokenRepository.save(stored);
            }
        });
    }

    /** Used on "log out everywhere", on PIN change, and when an account is suspended. */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        return refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required but unavailable", ex);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
