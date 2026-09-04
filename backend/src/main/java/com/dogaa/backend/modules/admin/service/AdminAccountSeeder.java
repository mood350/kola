package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.config.AdminSeedProperties;
import com.dogaa.backend.modules.admin.entity.AdminAccount;
import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.admin.repository.AdminAccountRepository;
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
            new Seed("sena.ametepe@dogaa.io", "Sena Amétépé", AdminRole.SUPER_ADMIN,
                    "Accès total · configuration produit"),
            new Seed("koffi.messan@dogaa.io", "Koffi Messan", AdminRole.COMPLIANCE_OFFICER,
                    "KYC, litiges, chargebacks (2e validation)"),
            new Seed("aya.djobo@dogaa.io", "Aya Djobo", AdminRole.CREDIT_ANALYST,
                    "Scoring, paliers de prêt, défauts"),
            new Seed("prisca.lawson@dogaa.io", "Prisca Lawson", AdminRole.SUPPORT,
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
}
