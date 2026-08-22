package com.kola.backend.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que le filtre de journalisation ne recopie jamais la valeur d'un
 * paramètre d'URL dans les logs.
 *
 * Ce test capture réellement la sortie Logback : se contenter de relire le
 * code n'aurait rien prouvé, et c'est précisément une lecture trop rapide de
 * ce filtre qui avait laissé passer l'écriture en clair des mots de passe.
 */
class RequestLoggingFilterRedactionTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger filterLogger;

    @BeforeEach
    void setUp() {
        appender = new ListAppender<>();
        appender.start();
        filterLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        filterLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        filterLogger.detachAppender(appender);
    }

    @Test
    void lesValeursDesParametresDUrlNeSontJamaisJournalisees() throws Exception {
        String loggedLine = logLineFor("/api/auth/reset-password",
                "token=482913&newPassword=MonSecret1");

        assertThat(loggedLine)
                .as("aucune trace du mot de passe ni du code dans les logs")
                .doesNotContain("MonSecret1")
                .doesNotContain("482913");

        assertThat(loggedLine)
                .as("les noms de paramètres restent lisibles pour le diagnostic")
                .contains("token=***")
                .contains("newPassword=***")
                .contains("/api/auth/reset-password");
    }

    @Test
    void uneRequeteSansQueryStringResteLisible() throws Exception {
        String loggedLine = logLineFor("/api/wallets", null);

        assertThat(loggedLine).contains("/api/wallets");
        assertThat(loggedLine).doesNotContain("?");
    }

    @Test
    void unParametreSansValeurNeCassePasLeFiltre() throws Exception {
        String loggedLine = logLineFor("/api/transactions", "refresh&page=2");

        assertThat(loggedLine).contains("refresh");
        assertThat(loggedLine).contains("page=***");
    }

    private String logLineFor(String uri, String queryString) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setQueryString(queryString);

        new RequestLoggingFilter().doFilter(
                request, new MockHttpServletResponse(), new MockFilterChain());

        List<ILoggingEvent> events = appender.list;
        assertThat(events).hasSize(1);
        return events.get(0).getFormattedMessage();
    }
}
