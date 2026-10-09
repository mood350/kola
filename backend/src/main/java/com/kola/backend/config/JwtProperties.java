package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /** Base64 (or raw) HMAC secret; must decode to 32 bytes or more. */
    private String secret;

    private String issuer = "kola";

    /** Short by design: revocation is handled by the refresh token, not the access token. */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(30);

    /**
     * Back-office sessions. Longer than a customer's access token because the admin front-end
     * stores one token and has no refresh flow: a 15-minute life would sign staff out mid-review.
     */
    private Duration adminTokenTtl = Duration.ofHours(8);
}
