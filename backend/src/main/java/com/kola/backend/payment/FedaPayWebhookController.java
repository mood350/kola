package com.kola.backend.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kola.backend.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Réception des notifications FedaPay.
 *
 * ═══ LA ROUTE EST PUBLIQUE, LA SIGNATURE EST LA SERRURE ═══
 *
 * FedaPay ne peut présenter aucun jeton Kola : l'endpoint doit donc être
 * accessible sans authentification (cf. SecurityConfig). Ce qui autorise le
 * mouvement d'argent, ici, c'est UNIQUEMENT la signature HMAC de l'en-tête
 * {@code X-FEDAPAY-SIGNATURE}. Sans secret configuré, tout est refusé — un
 * endpoint de crédit ouvert vaut moins que pas d'endpoint du tout.
 *
 * ═══ LE CORPS EST LU EN TEXTE BRUT ═══
 *
 * {@code @RequestBody String} et non un objet désérialisé : la signature porte
 * sur les octets reçus. Laisser Spring désérialiser puis re-sérialiser
 * changerait l'ordre des clés et les espaces, et invaliderait une signature
 * pourtant authentique.
 *
 * ═══ ON RÉPOND 200 MÊME QUAND ON NE FAIT RIEN ═══
 *
 * Un événement qui ne nous concerne pas (mise à jour de client, transaction
 * d'une autre application partageant le compte marchand) est acquitté. FedaPay
 * relance jusqu'à neuf fois puis DÉSACTIVE l'endpoint après dix échecs : rendre
 * une erreur sur un événement non pertinent finirait par couper la réception
 * des dépôts réels.
 *
 * Un 400 est réservé à un seul cas : la signature ne correspond pas. C'est le
 * seul moment où refuser est la bonne réponse.
 */
@RestController
@RequestMapping("/api/webhooks/fedapay")
@RequiredArgsConstructor
@Slf4j
public class FedaPayWebhookController {

    /** Événements qui soldent une demande d'encaissement. */
    private static final Set<String> APPROVAL_EVENTS = Set.of("transaction.approved");
    private static final Set<String> FAILURE_EVENTS = Set.of(
            "transaction.declined", "transaction.canceled");

    private final FedaPayProperties properties;
    private final WebhookSignatureVerifier signatureVerifier;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String payload,
            @RequestHeader(value = "X-FEDAPAY-SIGNATURE", required = false) String signature) {

        if (properties.getWebhookSecret().isBlank()) {
            log.error("Notification FedaPay reçue alors qu'aucun secret n'est configuré — refusée.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (!signatureVerifier.verify(payload, signature,
                properties.getWebhookSecret(), properties.getWebhookToleranceSeconds())) {
            /* Journalisé en WARN et non en ERROR : une signature invalide est
               au choix une tentative de fraude ou un secret désynchronisé après
               rotation. Les deux méritent d'être vus, aucun n'est une panne. */
            log.warn("Signature de notification FedaPay invalide — notification ignorée.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String name = event.path("name").asText("");
            JsonNode entity = event.path("entity");
            String providerTransactionId = entity.path("id").asText(null);

            if (providerTransactionId == null || providerTransactionId.isBlank()) {
                log.info("Notification FedaPay « {} » sans entité exploitable — acquittée sans effet.", name);
                return ResponseEntity.ok().build();
            }

            if (APPROVAL_EVENTS.contains(name)) {
                transactionService.settleMobileMoneyDeposit(providerTransactionId, true);
            } else if (FAILURE_EVENTS.contains(name)) {
                transactionService.settleMobileMoneyDeposit(providerTransactionId, false);
            } else {
                log.debug("Notification FedaPay « {} » ignorée (hors périmètre).", name);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            /* Le traitement a échoué APRÈS une signature valide : la
               notification est authentique et mérite d'être rejouée. On rend
               donc une erreur, contrairement aux cas « hors périmètre ». */
            log.error("Échec du traitement d'une notification FedaPay authentique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
