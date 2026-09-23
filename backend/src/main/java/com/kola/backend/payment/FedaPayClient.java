package com.kola.backend.payment;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Appels HTTP à l'API FedaPay.
 *
 * ═══ POURQUOI PAS LE SDK OFFICIEL ═══
 *
 * {@code com.fedapay:fedapay-java:1.0.1} existe, mais il ne couvre pas ce dont
 * on a besoin : {@code Transaction} y expose {@code generateToken()} et AUCUNE
 * méthode d'encaissement direct — il ne reste qu'un {@code
 * mobileMoneyModeAvailable(String)} dont la liste interne est déjà en retard sur
 * la documentation (ni togocel, ni free_sn, ni sbin). Son dépôt n'a pas bougé
 * depuis janvier 2022 et ne porte aucune licence. Il faudrait donc écrire ces
 * appels à la main de toute façon, en ajoutant une dépendance morte. On les
 * écrit ici, avec des types qui correspondent au reste du backend.
 *
 * ═══ TROIS APPELS, DANS CET ORDRE ═══
 *
 * <ol>
 *   <li>{@code POST /transactions} — déclare l'opération, renvoie son id ;</li>
 *   <li>{@code POST /transactions/{id}/token} — produit le jeton de paiement ;</li>
 *   <li>{@code POST /{mode}} — pousse la demande sur le téléphone du client,
 *       qui la valide par son code Mobile Money.</li>
 * </ol>
 *
 * Aucun de ces appels ne signifie « payé ». Le seul fait qui compte arrive plus
 * tard, par webhook.
 */
@Component
@Slf4j
public class FedaPayClient {

    private final FedaPayProperties properties;
    private final RestClient restClient;

    public FedaPayClient(FedaPayProperties properties, RestClient.Builder builder) {
        this.properties = properties;

        /* Des délais explicites, parce que le défaut est « attendre
           indéfiniment » : un opérateur Mobile Money qui ne répond pas
           immobiliserait sinon un thread de requête, et le client resterait
           devant un écran figé sans savoir si son argent est parti. */
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    /* ------------------------------------------------------------------ */

    /**
     * Ce que rend la création d'une transaction.
     *
     * L'URL vient avec l'identifiant, dans la MÊME réponse : c'est la page de
     * paiement hébergée, utilisable telle quelle quand le prélèvement direct
     * n'est pas ouvert sur le compte marchand. La lire ici évite un appel de
     * plus pour une donnée déjà reçue.
     *
     * @param paymentUrl nulle si le prestataire ne l'a pas jointe — les
     *                   anciennes versions de l'API ne la renvoient qu'avec le
     *                   jeton.
     */
    public record CreatedTransaction(String id, String paymentUrl) {}

    /** Jeton de paiement, et le lien hébergé qui l'accompagne. */
    public record PaymentToken(String token, String url) {}

    /** Étape 1 — déclare la transaction. Retourne son identifiant FedaPay. */
    public CreatedTransaction createTransaction(PaymentProvider.CollectCommand command) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("firstname", command.customerFirstName());
        customer.put("lastname", command.customerLastName());
        customer.put("email", command.customerEmail());
        customer.put("phone_number", Map.of(
                "number", command.phoneNumber(),
                "country", command.countryCode()
        ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", command.description());
        body.put("amount", toProviderAmount(command.amount()));
        body.put("currency", Map.of("iso", command.currency()));
        body.put("callback_url", properties.getCallbackUrl());
        /* La référence Kola voyage jusque chez le prestataire : c'est ce qui
           permet, devant un litige, de rapprocher les deux grands livres sans
           deviner. */
        body.put("merchant_reference", command.reference());
        body.put("custom_metadata", Map.of("kola_reference", command.reference()));
        body.put("customer", customer);

        JsonNode response = post("/transactions", body, "création de la transaction");
        JsonNode transaction = unwrap(response, "transaction");

        String id = text(transaction, "id");
        if (id == null) {
            throw new PaymentProviderException(
                    "Réponse FedaPay inexploitable : aucun identifiant de transaction.");
        }
        return new CreatedTransaction(id, text(transaction, "payment_url"));
    }

    /** Étape 2 — produit le jeton de paiement associé à la transaction. */
    public PaymentToken generateToken(String transactionId) {
        JsonNode response = post("/transactions/" + transactionId + "/token", null, "génération du jeton");
        JsonNode node = unwrap(response, "token");

        /* Deux formes possibles selon la version d'API : le jeton nu à la
           racine, ou enveloppé. On accepte les deux plutôt que de parier. */
        String token = text(node, "token");
        if (token == null) token = text(response, "token");

        String url = text(node, "url");
        if (url == null) url = text(response, "url");

        if (token == null) {
            throw new PaymentProviderException("Réponse FedaPay inexploitable : aucun jeton de paiement.");
        }
        return new PaymentToken(token, url);
    }

    /**
     * Étape 3 — pousse la demande de paiement sur le téléphone du client.
     *
     * ═══ LE MODE EST À LA RACINE, PAS SOUS /transactions ═══
     *
     * {@code POST /v1/{mode}}, et non {@code POST /v1/transactions/{mode}} : la
     * seconde forme répond 404 chez FedaPay (vérifié contre le bac à sable), ce
     * que la traduction d'erreur présentait comme « le prestataire a refusé
     * l'opération » — un refus imaginaire, qui envoyait chercher la panne du
     * côté du numéro ou du solde du client alors qu'aucune demande n'avait
     * jamais atteint l'opérateur. Les deux premières étapes réussissant, la
     * transaction restait « pending » chez eux et le dépôt n'aboutissait jamais.
     */
    public String sendNow(String token, MobileMoneyMode mode, String phoneNumber, String countryCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("phone_number", Map.of(
                "number", phoneNumber,
                "country", countryCode
        ));

        JsonNode response = post("/" + mode.getProviderCode(), body,
                "envoi de la demande " + mode.getLabel());

        /* La réponse documentée est une LISTE d'opérations. On lit le premier
           élément quand c'en est une, l'objet lui-même sinon. */
        JsonNode first = response.isArray() && !response.isEmpty() ? response.get(0) : response;
        String status = text(unwrap(first, "transaction"), "status");
        return status != null ? status : "pending";
    }

    /* ---- Versements sortants ----------------------------------------- */

    /**
     * Étape 1 du versement — déclare l'ordre. Il naît « pending », inerte.
     *
     * Le client est décrit en entier plutôt que par un identifiant : FedaPay
     * rapproche les clients par e-mail et met à jour la fiche existante. Envoyer
     * un identifiant supposerait de tenir une table de correspondance entre nos
     * utilisateurs et les leurs, pour un gain nul.
     */
    public String createPayout(PaymentProvider.PayoutCommand command) {
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("firstname", command.customerFirstName());
        customer.put("lastname", command.customerLastName());
        customer.put("email", command.customerEmail());
        customer.put("phone_number", Map.of(
                "number", command.phoneNumber(),
                "country", command.countryCode()
        ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", toProviderAmount(command.amount()));
        body.put("currency", Map.of("iso", command.currency()));
        body.put("mode", command.mode().getProviderCode());
        /* Description exigée par la conformité BCEAO sur les versements :
           un mouvement sortant doit porter son motif. */
        body.put("description", command.description());
        body.put("merchant_reference", command.reference());
        body.put("custom_metadata", Map.of("kola_reference", command.reference()));
        body.put("customer", customer);

        JsonNode response = post("/payouts", body, "création du versement");
        String id = text(unwrap(response, "payout"), "id");

        if (id == null) {
            throw new PaymentProviderException(
                    "Réponse FedaPay inexploitable : aucun identifiant de versement.");
        }
        return id;
    }

    /**
     * Étape 2 — démarre le versement.
     *
     * SÉPARÉE DE LA CRÉATION PAR L'API ELLE-MÊME, et c'est heureux : entre les
     * deux, l'ordre existe sans être parti. Si le démarrage échoue, on sait que
     * rien n'a bougé, là où un appel unique laisserait un doute.
     *
     * L'API attend un TABLEAU de versements sur un `PUT /payouts/start` — on
     * n'en envoie qu'un, mais la forme est imposée.
     */
    public void startPayout(String payoutId) {
        Map<String, Object> body = Map.of(
                "payouts", java.util.List.of(Map.of("id", payoutId))
        );
        send(org.springframework.http.HttpMethod.PUT, "/payouts/start", body,
                "démarrage du versement " + payoutId);
    }

    /**
     * Relit l'issue d'un versement.
     *
     * Aucun webhook ne couvre les versements — la documentation FedaPay ne liste
     * d'événements que pour les transactions et les clients. Le statut doit donc
     * être demandé, ce que fait le travail de réconciliation.
     */
    public PaymentProvider.PayoutStatus payoutStatus(String payoutId) {
        JsonNode response = send(org.springframework.http.HttpMethod.GET,
                "/payouts/" + payoutId, null, "lecture du versement " + payoutId);
        String status = text(unwrap(response, "payout"), "status");

        if (status == null) return PaymentProvider.PayoutStatus.PENDING;

        return switch (status) {
            case "sent" -> PaymentProvider.PayoutStatus.SENT;
            case "failed" -> PaymentProvider.PayoutStatus.FAILED;
            /* pending, started, processing : en cours. On ne conclut pas — un
               retrait déclaré échoué à tort recréditerait un portefeuille dont
               l'argent est pourtant parti. */
            default -> PaymentProvider.PayoutStatus.PENDING;
        };
    }

    /* ------------------------------------------------------------------ */

    /** Raccourci pour les appels POST, majoritaires. */
    private JsonNode post(String path, Object body, String what) {
        return send(org.springframework.http.HttpMethod.POST, path, body, what);
    }

    /**
     * Un appel au prestataire, quelle que soit la méthode.
     *
     * Tout passe par ici : le refus de partir sans configuration, la traduction
     * des erreurs, la distinction entre « refusé » et « injoignable ». Écrire un
     * second chemin pour les versements aurait fait diverger ces trois
     * comportements au premier correctif.
     */
    private JsonNode send(org.springframework.http.HttpMethod method,
                          String path, Object body, String what) {
        if (!properties.isUsable()) {
            throw new PaymentProviderException(
                    "Le paiement Mobile Money n'est pas configuré sur ce serveur.");
        }

        try {
            RestClient.RequestBodySpec spec = restClient.method(method).uri(path);
            JsonNode response = (body == null ? spec : spec.body(body))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, res) -> {
                        String payload = new String(res.getBody().readAllBytes());
                        /* Le corps d'erreur de FedaPay est journalisé, jamais
                           renvoyé au client : il peut contenir des détails de
                           compte marchand. L'utilisateur reçoit un message
                           générique, le support a le journal. */
                        log.error("FedaPay a refusé la {} — HTTP {} : {}",
                                what, res.getStatusCode(), payload);

                        int code = res.getStatusCode().value();

                        /* ═══ 401/403 N'EST PAS UN REFUS D'OPÉRATION ═══
                           C'est Kola qui n'est pas authentifié auprès du
                           prestataire : clé absente, révoquée, ou publique
                           là où la clé secrète est requise. Le dire
                           « opération refusée » envoie chercher le problème du
                           mauvais côté — chez l'utilisateur, son numéro ou son
                           solde — alors que rien de tout cela n'est en cause. */
                        if (code == 401 || code == 403) {
                            log.error("→ Clé API refusée. L'API serveur exige la clé SECRÈTE "
                                    + "(sk_...) ; une clé publique (pk_...) ne l'ouvre pas.");
                            throw new PaymentProviderException(
                                    "Le paiement Mobile Money n'est pas correctement configuré "
                                            + "sur ce serveur. Contactez le support.");
                        }

                        /* 5xx : le prestataire est en panne, l'opération n'a pas
                           été examinée. Réessayer a un sens, contrairement à un
                           refus métier. */
                        if (code >= 500) {
                            throw new PaymentProviderException(
                                    "Le prestataire de paiement est momentanément indisponible. "
                                            + "Réessayez dans quelques instants.");
                        }

                        throw new PaymentProviderException(
                                "Le prestataire de paiement a refusé l'opération.");
                    })
                    .body(JsonNode.class);

            if (response == null) {
                throw new PaymentProviderException("Réponse vide du prestataire de paiement.");
            }
            return response;

        } catch (PaymentProviderException alreadyMapped) {
            throw alreadyMapped;
        } catch (Exception networkFailure) {
            log.error("FedaPay injoignable lors de la {}", what, networkFailure);
            throw new PaymentProviderException(
                    "Le prestataire de paiement est injoignable. Réessayez dans un instant.",
                    networkFailure);
        }
    }

    /**
     * Retire l'enveloppe de réponse quand il y en a une.
     *
     * POURQUOI CETTE PRUDENCE : les deux documentations en ligne ne s'accordent
     * pas. L'ancienne montre des réponses enveloppées ({@code {"v1/transaction":
     * {...}}}), la référence actuelle montre l'objet nu. Plutôt que de parier
     * sur l'une et de découvrir l'erreur en production, on accepte les deux :
     * si une clé se termine par le nom attendu et contient un objet, on
     * descend dedans ; sinon on garde la racine.
     */
    private JsonNode unwrap(JsonNode response, String resource) {
        if (response == null || !response.isObject()) return response;

        var names = response.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (name.endsWith(resource) && response.get(name).isObject()) {
                return response.get(name);
            }
        }
        return response;
    }

    private String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Montant tel que l'attend FedaPay : un entier.
     *
     * Le franc CFA n'a pas de subdivision — il n'existe pas de centime de XOF.
     * Un montant à décimales ne peut donc pas être encaissé, et l'arrondir en
     * silence ferait diverger le grand livre de ce qui a réellement été prélevé.
     * On refuse, plutôt que de perdre ou d'inventer des francs.
     */
    static long toProviderAmount(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() > 0) {
            throw new PaymentProviderException(
                    "Le franc CFA ne se divise pas : un montant à décimales ne peut pas être encaissé.");
        }
        return stripped.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
    }
}
