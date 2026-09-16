package com.kola.backend.modules.admin.service;

import com.kola.backend.config.AdminSeedProperties;
import com.kola.backend.modules.admin.entity.AdminAccount;
import com.kola.backend.modules.admin.entity.AdminRole;
import com.kola.backend.modules.admin.repository.AdminAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates the four reference back-office accounts on an empty database (BACKEND.md 4.2).
 *
 * <p>Without this the admin front-end cannot sign in at all, so there is nobody to create the first
 * account — the usual chicken-and-egg of staff authentication.
 *
 * <p>It only ever runs when the table is empty, never overwrites an existing password, and shouts
 * in the log about the shared seed password. <b>Turn it off in production</b> with
 * {@code app.admin.seed.enabled=false} and create staff accounts deliberately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder implements ApplicationRunner {

    private record Seed(String email, String name, AdminRole role, String scope) {
    }

    private static final List<Seed> ACCOUNTS = List.of(
            new Seed("sena.ametepe@kola.io", "Sena Amétépé", AdminRole.SUPER_ADMIN,
                    "Accès total · configuration produit"),
            new Seed("koffi.messan@kola.io", "Koffi Messan", AdminRole.COMPLIANCE_OFFICER,
                    "KYC, litiges, chargebacks (2e validation)"),
            new Seed("aya.djobo@kola.io", "Aya Djobo", AdminRole.CREDIT_ANALYST,
                    "Scoring, paliers de prêt, défauts"),
            new Seed("prisca.lawson@kola.io", "Prisca Lawson", AdminRole.SUPPORT,
                    "Tickets, consultation comptes (lecture seule)"));

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        seedDemoAccounts();
        seedOwner();
    }

    /**
     * The four reference roles, created only on an empty table so that nobody's edited account is
     * ever overwritten.
     */
    private void seedDemoAccounts() {
        if (adminAccountRepository.count() > 0) {
            // Someone already manages these accounts; never touch them again.
            return;
        }

        ACCOUNTS.forEach(seed -> adminAccountRepository.save(AdminAccount.builder()
                .email(seed.email())
                .name(seed.name())
                .role(seed.role())
                .scope(seed.scope())
                .passwordHash(passwordEncoder.encode(properties.getPassword()))
                .enabled(true)
                .build()));

        log.warn("""
                Seeded {} back-office accounts with the shared development password "{}".
                Sign in at POST /api/v1/admin/auth/login — for example {}.
                Set app.admin.seed.enabled=false and change these passwords before production.""",
                ACCOUNTS.size(), properties.getPassword(), ACCOUNTS.get(0).email());
    }

    /**
     * The operator's own super-admin.
     *
     * <p>Checked on every start rather than only on an empty table: the demo accounts exist after
     * the very first boot, so an all-or-nothing seeder can never add anyone again — which is
     * exactly when someone needs their own account.
     *
     * <p>An existing account is left completely alone, password included. Re-hashing it on every
     * restart would quietly undo a password the owner had changed.
     */
    private void seedOwner() {
        AdminSeedProperties.Owner owner = properties.getOwner();
        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            return;
        }

        String email = owner.getEmail().strip().toLowerCase();
        if (adminAccountRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        String password = owner.getPassword() == null || owner.getPassword().isBlank()
                ? properties.getPassword()
                : owner.getPassword();

        adminAccountRepository.save(AdminAccount.builder()
                .email(email)
                .name(owner.getName())
                .role(AdminRole.SUPER_ADMIN)
                .scope("Accès total · compte propriétaire")
                .passwordHash(passwordEncoder.encode(password))
                .enabled(true)
                .build());

        log.warn("Created the owner super-admin account {}. Sign in at POST /api/v1/admin/auth/login.",
                email);
    }
}
