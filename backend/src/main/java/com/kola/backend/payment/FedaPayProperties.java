package com.kola.backend.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration FedaPay.
 *
 * TOUT EST OPTIONNEL, ET C'EST DÉLIBÉRÉ : sans clé, l'application démarre
 * normalement et seule la route d'encaissement répond « prestataire non
 * configuré ». Exiger la clé au démarrage rendrait le projet impossible à
 * lancer pour quelqu'un qui travaille sur les coffres ou le scoring, et ferait
 * échouer la suite de tests.
 *
 * L'URL par défaut est celle du BAC À SABLE. Passer en production demande un
 * geste explicite (changer {@code FEDAPAY_BASE_URL} et la clé) : personne ne
 * doit encaisser de l'argent réel par simple oubli de configuration.
 */
@Component
@ConfigurationProperties(prefix = "fedapay")
@Getter
@Setter
public class FedaPayProperties {

    /** Coupe l'intégration sans retirer la configuration. */
    private boolean enabled = false;

    /** Racine de l'API, préfixe de version compris. */
    private String baseUrl = "https://sandbox-api.fedapay.com/v1";

    /** Clé secrète : sk_sandbox_… en bac à sable, sk_live_… en production. */
    private String apiKey = "";

    /** Secret de l'endpoint webhook (wh_sandbox_…), qui signe les notifications. */
    private String webhookSecret = "";

    /** Page vers laquelle FedaPay renvoie le client après paiement. */
    private String callbackUrl = "http://localhost:3002/transactions";

    /** Délai d'attente d'un appel sortant. */
    private int timeoutSeconds = 15;

    /**
     * Tolérance d'horodatage des webhooks, en secondes.
     *
     * Une signature valide mais vieille de plusieurs heures est probablement
     * rejouée : au-delà de ce délai, la notification est refusée. 300 secondes
     * est la valeur retenue par les bibliothèques FedaPay.
     */
    private long webhookToleranceSeconds = 300;

    /** Vrai quand l'intégration est utilisable — activée ET munie d'une clé. */
    public boolean isUsable() {
        return enabled && !apiKey.isBlank();
    }
}
