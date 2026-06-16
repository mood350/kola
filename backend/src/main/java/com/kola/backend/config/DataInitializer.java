package com.kola.backend.config;

import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialise les données par défaut en base de données au démarrage de l'application.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        
        // Création du rôle Administrateur s'il n'existe pas
        if (roleRepository.findByRoleName("Administrateur").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setRoleName("Administrateur");
            roleRepository.save(adminRole);
        }

        // Création du rôle Client s'il n'existe pas
        if (roleRepository.findByRoleName("Client").isEmpty()) {
            Role clientRole = new Role();
            clientRole.setRoleName("Client");
            roleRepository.save(clientRole);
        }
    }
}
