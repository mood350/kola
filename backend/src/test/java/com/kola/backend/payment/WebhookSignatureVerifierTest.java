package com.kola.backend.payment;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérification de la signature des webhooks.
 *
 * CE QUE CES TESTS DÉFENDENT : l'endpoint webhook est public et crédite des
 * wallets. La signature est la seule chose qui distingue une notification de
 * FedaPay d'une requête fabriquée par n'importe qui connaissant l'URL. Chaque
 * cas ci-dessous correspond à une façon concrète de s'y introduire.
 */
class WebhookSignatureVerifierTest {

    private static final String SECRET = "wh_sandbox_secret_de_test";
    private static final String PAYLOAD =
            "{\"name\":\"transaction.approved\",\"entity\":{\"id\":\"4242\"}}";

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    private String headerFor(String payload, String secret, long timestamp) {
        String signature = WebhookSignatureVerifier.computeHmacSha256(secret, timestamp + "." + payload);
        return "t=" + timestamp + ",s=" + signature;
    }

    @Test
    void uneSignatureAuthentiqueEstAcceptee() {
        long now = Instant.now().getEpochSecond();

        assertThat(verifier.verify(PAYLOAD, headerFor(PAYLOAD, SECRET, now), SECRET, 300))
                .isTrue();
    }

    @Test
    void unCorpsModifieApresSignatureEstRejete() {
        long now = Instant.now().getEpochSecond();
        String header = headerFor(PAYLOAD, SECRET, now);

        // Le scénario redouté : intercepter une notification authentique et
        // remplacer l'identifiant de transaction par un autre.
        String falsifie = PAYLOAD.replace("4242", "9999");

        assertThat(verifier.verify(falsifie, header, SECRET, 300))
                .as("la signature porte sur le corps : le modifier doit l'invalider")
                .isFalse();
    }

    @Test
    void unSecretDifferentEstRejete() {
        long now = Instant.now().getEpochSecond();
        String header = headerFor(PAYLOAD, "wh_secret_de_lattaquant", now);

        assertThat(verifier.verify(PAYLOAD, header, SECRET, 300)).isFalse();
    }

    @Test
    void uneNotificationTropAncienneEstRejetee() {
        long ilYAUneHeure = Instant.now().getEpochSecond() - 3600;
        String header = headerFor(PAYLOAD, SECRET, ilYAUneHeure);

        assertThat(verifier.verify(PAYLOAD, header, SECRET, 300))
                .as("une notification authentique rejouée demain ne doit pas recréditer un dépôt")
                .isFalse();

        assertThat(verifier.verify(PAYLOAD, header, SECRET, 0))
                .as("tolérance à 0 : le contrôle d'âge est désactivé, la signature reste valable")
                .isTrue();
    }

    @Test
    void unEnTeteMalFormeOuAbsentEstRejete() {
        assertThat(verifier.verify(PAYLOAD, null, SECRET, 300)).isFalse();
        assertThat(verifier.verify(PAYLOAD, "", SECRET, 300)).isFalse();
        assertThat(verifier.verify(PAYLOAD, "n'importe quoi", SECRET, 300)).isFalse();
        assertThat(verifier.verify(PAYLOAD, "t=abc,s=def", SECRET, 300)).isFalse();
        // Signature seule, sans horodatage : le rejeu redeviendrait possible.
        assertThat(verifier.verify(PAYLOAD,
                "s=" + WebhookSignatureVerifier.computeHmacSha256(SECRET, PAYLOAD), SECRET, 300))
                .isFalse();
    }

    @Test
    void sansSecretConfigureRienNEstAccepte() {
        long now = Instant.now().getEpochSecond();
        // En-tête parfaitement valide... mais le serveur n'a pas de secret.
        String header = headerFor(PAYLOAD, SECRET, now);

        assertThat(verifier.verify(PAYLOAD, header, "", 300))
                .as("un secret absent ne doit jamais valoir autorisation : la vérification refuse avant tout calcul")
                .isFalse();
        assertThat(verifier.verify(PAYLOAD, header, null, 300)).isFalse();
    }
}
