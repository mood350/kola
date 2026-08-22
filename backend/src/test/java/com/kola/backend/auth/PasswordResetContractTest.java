package com.kola.backend.auth;

import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.token.Token;
import com.kola.backend.token.TokenRepository;
import com.kola.backend.token.TokenType;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrat de POST /api/auth/reset-password après le passage des secrets du
 * query param vers le corps de la requête.
 *
 * Vérifie à la fois que le flux fonctionne toujours de bout en bout (le mot de
 * passe est réellement changé, le code consommé) et que l'ancienne forme n'est
 * plus acceptée — sans quoi la fuite dans les logs resterait atteignable.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String ANCIEN_MOT_DE_PASSE = "Ancien123";
    private static final String NOUVEAU_MOT_DE_PASSE = "Nouveau456";

    private User user;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        user = userRepository.save(User.builder()
                .firstName("Reset")
                .lastName("Contract")
                .email("reset-" + unique + "@kola.test")
                .phoneNumber("+225" + String.format("%08d", Math.abs(unique.hashCode()) % 100_000_000))
                .countryCode("CI")
                .password(passwordEncoder.encode(ANCIEN_MOT_DE_PASSE))
                .roles(List.of(clientRole))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build());
    }

    @AfterEach
    void tearDown() {
        tokenRepository.deleteAll(
                tokenRepository.findAll().stream()
                        .filter(t -> t.getUser().getId().equals(user.getId()))
                        .toList());
        userRepository.delete(user);
    }

    @Test
    void leCodeEtLeMotDePasseSontAcceptesDansLeCorpsDeLaRequete() throws Exception {
        String code = givenResetToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(code, NOUVEAU_MOT_DE_PASSE)))
                .andExpect(status().isOk());

        User after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(NOUVEAU_MOT_DE_PASSE, after.getPassword()))
                .as("le mot de passe doit réellement avoir été changé")
                .isTrue();

        assertThat(tokenRepository.findValidToken(code, TokenType.PASSWORD_RESET, LocalDateTime.now()))
                .as("le code doit être consommé, pas réutilisable")
                .isEmpty();
    }

    @Test
    void lAncienneFormeEnQueryParamNEstPlusAcceptee() throws Exception {
        String code = givenResetToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", code)
                        .param("newPassword", NOUVEAU_MOT_DE_PASSE))
                .andExpect(status().isBadRequest());

        User after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(ANCIEN_MOT_DE_PASSE, after.getPassword()))
                .as("le mot de passe ne doit pas avoir changé")
                .isTrue();
    }

    @Test
    void unMotDePasseTropFaibleEstRefuse() throws Exception {
        String code = givenResetToken();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"faible"}
                                """.formatted(code)))
                .andExpect(status().isBadRequest());

        User after = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(ANCIEN_MOT_DE_PASSE, after.getPassword()))
                .as("ce flux ne doit pas permettre de contourner la politique de mot de passe")
                .isTrue();
    }

    private String givenResetToken() {
        String code = String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000);
        tokenRepository.save(Token.builder()
                .token(code)
                .tokenType(TokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build());
        return code;
    }
}
