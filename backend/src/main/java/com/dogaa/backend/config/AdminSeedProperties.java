package com.dogaa.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bootstrap of the first back-office accounts. Must be disabled in production. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin.seed")
public class AdminSeedProperties {

    private boolean enabled = true;

    /** Shared password given to every seeded account. Change it, then disable the seeder. */
    private String password = "DogaaAdmin2026!";
}
