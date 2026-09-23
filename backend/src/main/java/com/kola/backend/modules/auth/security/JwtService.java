package com.kola.backend.modules.auth.security;

import com.kola.backend.common.enums.Role;
import com.kola.backend.modules.admin.entity.AdminAccount;
import com.kola.backend.modules.admin.entity.AdminRole;
import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.config.JwtProperties;
import com.kola.backend.modules.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates the stateless access token. Refresh tokens are deliberately not JWTs:
 * they are opaque, stored hashed, and revocable — see {@code RefreshTokenService}.
 */
@Slf4j
@Service
public class JwtService {

    private static final String CLAIM_PHONE = "phone";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_ADMIN = "admin";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secret = decodeSecret(properties.getSecret());
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must decode to at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_PHONE, user.getPhone())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenTtl())))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Token for a back-office session. Same signing key, different {@code typ} claim, so a
     * customer token can never be presented as an administrator's and vice versa.
     */
    public String generateAdminToken(AdminAccount admin) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(admin.getId().toString())
                .claim(CLAIM_EMAIL, admin.getEmail())
                .claim(CLAIM_NAME, admin.getName())
                .claim(CLAIM_ROLE, admin.getRole().name())
                .claim(CLAIM_TYPE, TYPE_ADMIN)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAdminTokenTtl())))
                .signWith(signingKey)
                .compact();
    }

    /** @return the administrator carried by the token, or empty if it is not a valid admin token. */
    public Optional<CurrentAdmin> parseAdminToken(String token) {
        return claims(token)
                .filter(claims -> TYPE_ADMIN.equals(claims.get(CLAIM_TYPE, String.class)))
                .map(claims -> new CurrentAdmin(
                        UUID.fromString(claims.getSubject()),
                        claims.get(CLAIM_EMAIL, String.class),
                        claims.get(CLAIM_NAME, String.class),
                        AdminRole.valueOf(claims.get(CLAIM_ROLE, String.class))));
    }

    public long adminTokenTtlSeconds() {
        return properties.getAdminTokenTtl().toSeconds();
    }

    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    /** @return the principal carried by the token, or empty if the token is invalid or expired. */
    public Optional<CurrentUser> parseAccessToken(String token) {
        return claims(token)
                .filter(claims -> TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class)))
                .map(claims -> new CurrentUser(
                        UUID.fromString(claims.getSubject()),
                        claims.get(CLAIM_PHONE, String.class),
                        Role.valueOf(claims.get(CLAIM_ROLE, String.class))));
    }

    private Optional<Claims> claims(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Accepts either a base64 secret or a raw passphrase, so local setups stay easy. */
    private static byte[] decodeSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            return secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
