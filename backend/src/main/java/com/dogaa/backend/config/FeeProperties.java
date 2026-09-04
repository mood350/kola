package com.dogaa.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Commission rates, as percentages of the transferred amount (DOGAA.md 5.3.A).
 * Bound from {@code app.fees.*}; the defaults here are the rates quoted in the spec, so the
 * app still runs with no override.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.fees")
public class FeeProperties {

    /** Peer-to-peer transfer. Spec: 1.5%. */
    private BigDecimal p2pPercent = new BigDecimal("1.5");

    /** Partner-merchant payment. Spec: "fixed or variable" — 1% taken as the default. */
    private BigDecimal merchantPercent = new BigDecimal("1.0");

    /** Utility bill payment (DOGAA.md 4.6.1) — not quoted in the spec; treated like cash-out. */
    private BigDecimal billPaymentPercent = new BigDecimal("1.0");

    /** Cash-out to another Mobile Money network. Spec: 1%. */
    private BigDecimal cashOutPercent = new BigDecimal("1.0");

    /** Cash-in. Deliberately free to pull deposits onto Dogaa. */
    private BigDecimal cashInPercent = BigDecimal.ZERO;
}
