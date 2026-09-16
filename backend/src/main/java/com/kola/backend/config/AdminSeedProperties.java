package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bootstrap of the first back-office accounts. Must be disabled in production. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin.seed")
public class AdminSeedProperties {

    private boolean enabled = true;

    /** Shared password given to every seeded demo account. Change it, then disable the seeder. */
    private String password = "KolaAdmin2026!";

    /**
     * A personal super-admin, created in addition to the demo accounts.
     *
     * <p>Configured rather than hard-coded because {@code application.properties} is gitignored:
     * a real person's address and password have no business in committed source. Leave
     * {@code owner-email} blank and nothing is created.
     *
     * <p>Unlike the demo accounts, this one is checked on every start, so it can be added to a
     * database that already has admins — which is the situation anyone hits after the first boot.
     */
    private Owner owner = new Owner();

    @Getter
    @Setter
    public static class Owner {

        /** Blank disables it entirely. */
        private String email = "";

        private String name = "Administrateur";

        /** Falls back to the shared seed password when left blank. */
        private String password = "";
    }
}
