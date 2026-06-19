package com.kola.backend.config;

import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.user.KycLevel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createRoleIfMissing("ADMIN");
        createRoleIfMissing("CLIENT");
        // J'ai commenté ces deux lignes : elles semblent servir à migrer d'anciennes données
        // (legacyRoleName "Administrateur" / "Client"). Si tu n'as pas ces anciens rôles en BDD,
        // ça va faire des requêtes inutiles voire des erreurs. À garder uniquement si c'est une vraie migration.
        // migrateRoleMembership("Administrateur", "ADMIN");
        // migrateRoleMembership("Client", "CLIENT");
        createDefaultAdminIfMissing();
    }

    private void createRoleIfMissing(String roleName) {
        if (roleRepository.findByRoleName(roleName).isEmpty()) {
            Role role = new Role();
            role.setRoleName(roleName);
            roleRepository.save(role);
        }
    }

    /* ... gardé si tu en as vraiment besoin ...
    private void migrateRoleMembership(String legacyRoleName, String targetRoleName) { ... }
    */

    private void createDefaultAdminIfMissing() {
        // Sécurité : le .get() sur Optional est safe grâce au isPresent() au dessus
        if (userRepository.findByEmail("admin@kola.com").isPresent()) {
            return;
        }

        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN introuvable en BDD"));

        User admin = User.builder()
                .firstName("Kola")
                .lastName("Admin")
                .email("admin@kola.com")
                .phoneNumber("+22890262138")
                .countryCode("TG")
                .kycLevel(KycLevel.TIER_2)
                .password(passwordEncoder.encode("Kola1234"))
                .roles(List.of(adminRole))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(admin);
    }
}