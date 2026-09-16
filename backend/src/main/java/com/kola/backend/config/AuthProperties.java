package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.auth")
public class AuthProperties {

    /** Calling code assumed when a user types a local phone number. 228 = Togo. */
    private String defaultCallingCode = "228";

    /** Wrong PINs tolerated before the account is locked. */
    private int maxPinAttempts = 5;

    private Duration lockDuration = Duration.ofMinutes(15);
}
