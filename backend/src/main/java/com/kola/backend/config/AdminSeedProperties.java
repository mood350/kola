package com.kola.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap of the first back-office accounts.
 *
 * <p>Off by default and with no default password: a start that nobody configured must not create
 * administrators whose credentials are readable in the repository. Turn it on locally in
 * {@code application.properties} and give it a password of your own.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin.seed")
public class AdminSeedProperties {

    private boolean enabled = false;

    /** Shared password given to every seeded demo account. Blank = the demo accounts are not created. */
    private String password = "";

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
