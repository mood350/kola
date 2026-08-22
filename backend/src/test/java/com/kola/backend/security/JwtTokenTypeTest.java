package com.kola.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les tokens d'accès et de rafraîchissement doivent cesser d'être
 * interchangeables.
 *
 * Signés avec la même clé, le même sujet et sans claim distinctif, ils
 * l'étaient totalement : un refresh token volé servait de laissez-passer
 * pendant 7 jours, et un access token permettait de battre monnaie sur
 * /api/auth/refresh-token.
 */
@SpringBootTest
class JwtTokenTypeTest {

    @Autowired
    private JwtService jwtService;

    private final UserDetails client = User.withUsername("client@kola.test")
            .password("peu-importe")
            .authorities("CLIENT")
            .build();

    @Test
    void unTokenDAccesNEstPasAccepteCommeRefreshToken() {
        String accessToken = jwtService.generateToken(client);

        assertThat(jwtService.isTokenValid(accessToken, client)).isTrue();
        assertThat(jwtService.isRefreshTokenValid(accessToken, client))
                .as("sinon un access token permet de régénérer des tokens indéfiniment")
                .isFalse();
    }

    @Test
    void unRefreshTokenNEstPasAccepteCommeTokenDAcces() {
        String refreshToken = jwtService.generateRefreshToken(client);

        assertThat(jwtService.isRefreshTokenValid(refreshToken, client)).isTrue();
        assertThat(jwtService.isTokenValid(refreshToken, client))
                .as("sinon un refresh token vaut un accès complet de 7 jours")
                .isFalse();
    }

    @Test
    void unTokenNEstValableQuePourSonProprietaire() {
        String accessToken = jwtService.generateToken(client);
        UserDetails autre = User.withUsername("autre@kola.test")
                .password("peu-importe")
                .authorities("CLIENT")
                .build();

        assertThat(jwtService.isTokenValid(accessToken, autre)).isFalse();
    }
}
