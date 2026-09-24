package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bootstrap of a single test client account for local development. Must be disabled in production. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.dev.seed")
public class DevUserSeedProperties {

    private boolean enabled = true;

    /** Local-format phone number, read with {@code app.security.auth.default-calling-code}. */
    private String phone = "90000000";

    /** Not run through {@link com.kola.backend.modules.auth.security.PinPolicy}: a memorable dev PIN, not one a real user could set. */
    private String pin = "1234";

    private String firstName = "Kola";

    private String lastName = "Testeur";

    private LocalDate dateOfBirth = LocalDate.of(1995, 1, 1);

    private String address = "Quartier Bè";

    private String city = "Lomé";

    /** ISO 3166-1 alpha-2. */
    private String country = "TG";

    /** Brings the account up to every credit gate on each start: TIER_2, collateral, score. */
    private boolean creditReady = true;

    /** Savings balance topped up to on each start; it is the collateral, so it sets the loan size. */
    private BigDecimal savingsBalance = new BigDecimal("50000");

    /** Published score restored on each start when the nightly rescoring pulled it lower. */
    private int creditScore = 75;
}
