package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.otp")
public class OtpProperties {

    private int codeLength = 6;

    /** Short enough that an intercepted SMS goes stale quickly. */
    private Duration ttl = Duration.ofMinutes(5);

    /** Wrong codes tolerated before the challenge is burned and a new one must be requested. */
    private int maxAttempts = 5;

    /** Minimum delay between two sends for the same number, so the API cannot be used to spam by SMS. */
    private Duration resendCooldown = Duration.ofSeconds(60);

    /** How long the proof of verification stays usable for finishing the registration. */
    private Duration verificationTokenTtl = Duration.ofMinutes(15);
}
