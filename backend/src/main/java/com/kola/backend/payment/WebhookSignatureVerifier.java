package com.kola.backend.payment;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Vérification de la signature des webhooks FedaPay.
 *
 * ═══ CE QUE CETTE CLASSE PROTÈGE ═══
 *
 * L'endpoint webhook est PUBLIC : il doit l'être, FedaPay ne peut pas présenter
 * de jeton Kola. Sans cette vérification, n'importe qui connaissant l'URL
 * pourrait poster « transaction approuvée » et créditer le compte de son choix.
 * C'est donc la signature — et elle seule — qui autorise un mouvement d'argent
 * ici.
 *
 * ═══ L'ALGORITHME ═══
 *
 * L'en-tête {@code X-FEDAPAY-SIGNATURE} vaut {@code t=<horodatage>,s=<signature>}.
 * La signature est un HMAC-SHA256, calculé avec le secret de l'endpoint sur la
 * chaîne {@code <horodatage>.<corps brut>}. Le corps doit être celui reçu, OCTET
 * POUR OCTET : re-sérialiser le JSON change les espaces et l'ordre des clés, et
 * invalide la signature. C'est pour cela que le contrôleur lit une {@code String}
 * et non un objet désérialisé.
 *
 * ═══ L'HORODATAGE N'EST PAS DÉCORATIF ═══
 *
 * Il entre dans le calcul, donc il ne peut pas être modifié sans invalider la
 * signature. Le refuser au-delà d'une tolérance ferme la fenêtre de rejeu : une
 * notification authentique interceptée ne pourra pas être renvoyée demain pour
 * recréditer le même dépôt.
 *
 * Implémentation reprise de {@code WebhookSignature} du SDK FedaPay, qui est la
 * seule partie de cette bibliothèque qui vaille la peine d'être empruntée.
 */
@Component
public class WebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * @param payload   corps brut de la requête, tel que reçu
     * @param header    contenu de l'en-tête X-FEDAPAY-SIGNATURE
     * @param secret    secret de l'endpoint (wh_sandbox_…)
     * @param tolerance âge maximal accepté, en secondes ; 0 désactive le contrôle
     * @return vrai si la notification vient bien de FedaPay et n'est pas périmée
     */
    public boolean verify(String payload, String header, String secret, long tolerance) {
        if (payload == null || header == null || secret == null || secret.isBlank()) {
            return false;
        }

        long timestamp = -1;
        String signature = null;

        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            switch (pair[0].trim()) {
                case "t" -> timestamp = parseTimestamp(pair[1].trim());
                case "s" -> signature = pair[1].trim();
                default -> { /* champ inconnu : ignoré, pas une erreur */ }
            }
        }

        if (timestamp <= 0 || signature == null) return false;

        if (tolerance > 0 && Math.abs(Instant.now().getEpochSecond() - timestamp) > tolerance) {
            return false;
        }

        String expected = computeHmacSha256(secret, timestamp + "." + payload);

        /* Comparaison à temps constant : un `equals` classique s'arrête au
           premier octet différent, et la durée de l'appel révèle alors combien
           de caractères sont corrects. De quoi reconstituer une signature
           valide, octet par octet. */
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    static String computeHmacSha256(String key, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // Ni l'algorithme ni la clé ne peuvent manquer sur une JVM standard.
            throw new IllegalStateException("Calcul HMAC-SHA256 impossible", e);
        }
    }

    private long parseTimestamp(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
