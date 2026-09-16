package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** QR payment codes (modules/qr), bound from {@code app.qr.*}. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.qr")
public class QrProperties {

    /**
     * Prefix of what the QR actually contains. A https URL rather than a custom scheme so that a
     * plain camera app lands somewhere explanatory instead of showing an unusable string; the
     * mobile app intercepts it as a deep link.
     */
    private String payloadBaseUrl = "https://kola.app/p/";

    /** How long a payment request stays scannable when the caller does not say. */
    private Duration defaultRequestTtl = Duration.ofHours(24);

    /** Upper bound on a requested TTL: an "expiring" code that lives a year does not expire. */
    private Duration maxRequestTtl = Duration.ofDays(7);

    /** Rendered PNG side, in pixels. */
    private int defaultImageSize = 512;

    private int maxImageSize = 1024;

    /**
     * Bytes of randomness behind a code. 16 bytes is 128 bits: a code has to be unguessable, or
     * scanning becomes a way to enumerate accounts.
     */
    private int codeBytes = 16;
}
