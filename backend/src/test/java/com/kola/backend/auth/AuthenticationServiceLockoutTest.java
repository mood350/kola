package com.kola.backend.auth;

import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test d'intégration du verrouillage automatique après échecs répétés.
 *
 * Volontairement PAS un test Mockito comme les autres services : le bug qu'il
 * couvre est un rollback de transaction. Avec des mocks, `verify(save)` passait
 * au vert alors qu'en réel l'UPDATE était annulé par la propagation de
 * BadCredentialsException. Il faut donc une vraie transaction (@SpringBootTest,
 * sans @Transactional sur la classe de test — sinon la transaction du test
 * engloberait celle du service et masquerait à nouveau le problème).
 */
@SpringBootTest
class AuthenticationServiceLockoutTest {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "Test1234";
    private static final String WRONG_PASSWORD = "Wrong1234";

    // Email unique par exécution : la clé du rate limiter LOGIN est
    // "ip:email" et le bucket est en mémoire pour toute la vie de la JVM.
    // Un email fixe ferait échouer le test au 2e run dans le même contexte.
    private String email;
    private String ip;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        email = "lockout-" + unique + "@kola.test";
        ip = "203.0.113." + (Math.abs(unique.hashCode()) % 250 + 1);

        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        userRepository.save(User.builder()
                .firstName("Lock")
                .lastName("Out")
                .email(email)
                .phoneNumber("+228" + String.format("%08d", Math.abs(unique.hashCode()) % 100_000_000))
                .countryCode("TG")
                .password(passwordEncoder.encode(PASSWORD))
                .roles(List.of(clientRole))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build());
    }

    @AfterEach
    void tearDown() {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }

    @Test
    void leCompteurDEchecsEstPersisteMalgreLExceptionDAuthentification() {
        // Une seule tentative échouée doit laisser une trace en base.
        // C'est le cœur du bug : l'exception levée juste après le save()
        // annulait la transaction et donc l'incrément.
        attemptLoginWithWrongPassword();

        assertThat(reload().getFailedLoginAttempts())
                .as("le compteur doit survivre au BadCredentialsException")
                .isEqualTo(1);
    }

    @Test
    void leCompteEstVerrouilleApresCinqTentativesEchouees() {
        for (int i = 1; i <= 4; i++) {
            attemptLoginWithWrongPassword();

            User user = reload();
            assertThat(user.getFailedLoginAttempts()).isEqualTo(i);
            assertThat(user.isAccountLocked())
                    .as("pas encore verrouillé au bout de %d tentative(s)", i)
                    .isFalse();
        }

        attemptLoginWithWrongPassword(); // 5e = seuil MAX_FAILED_ATTEMPTS

        User locked = reload();
        assertThat(locked.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(locked.isAccountLocked()).isTrue();
        assertThat(locked.getLockedAt()).isNotNull();
    }

    @Test
    void unCompteVerrouilleRefuseLeBonMotDePasse() {
        for (int i = 0; i < 5; i++) {
            attemptLoginWithWrongPassword();
        }

        // IP différente : le bucket de rate limiting LOGIN (5/15 min, clé
        // "ip:email") est épuisé par les 5 tentatives ci-dessus. On veut
        // vérifier le verrou du compte, pas le rate limiter.
        assertThatThrownBy(() -> authenticationService.authenticate(
                new AuthenticationRequest(email, PASSWORD), "198.51.100.7", "JUnit"))
                .isInstanceOf(LockedException.class);
    }

    private void attemptLoginWithWrongPassword() {
        assertThatThrownBy(() -> authenticationService.authenticate(
                new AuthenticationRequest(email, WRONG_PASSWORD), ip, "JUnit"))
                .isInstanceOf(BadCredentialsException.class);
    }

    private User reload() {
        return userRepository.findByEmail(email).orElseThrow();
    }
}
