package com.kola.backend.payment;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * Pousser la demande de débit sur le téléphone, plutôt que d'ouvrir une page.
     *
     * ═══ FAUX PAR DÉFAUT, ET CE N'EST PAS DE LA PRUDENCE EXCESSIVE ═══
     *
     * Le prélèvement sans redirection ({@code POST /v1/{mode}}) est une
     * AUTORISATION COMMERCIALE, pas un réglage : un compte marchand qui ne l'a
     * pas obtenue reçoit un 400 « Opération non autorisée » sur tous les
     * opérateurs, alors même que sa clé est valide et que la transaction vient
     * d'être créée. Activer ce chemin par défaut ferait échouer tous les dépôts
     * de tout compte neuf, avec un message qui accuse le client.
     *
     * Le repli — la page de paiement hébergée — fonctionne sans autorisation
     * particulière et aboutit au MÊME webhook {@code transaction.approved}.
     * Seule l'étape de validation change de main.
     *
     * À passer à vrai le jour où FedaPay confirme l'activation, et pas avant :
     * c'est un basculement d'une ligne, vérifiable en une tentative de dépôt.
     */
    private boolean directCharge = false;

    /**
     * Ouvrir les retraits vers Mobile Money (versements sortants).
     *
     * ═══ FAUX PAR DÉFAUT, POUR LA MÊME RAISON QUE {@link #directCharge} ═══
     *
     * {@code POST /v1/payouts} répond 403 « Opération non autorisée » tant que
     * FedaPay n'a pas ouvert les versements sur le compte marchand — clé valide
     * ou non, cela ne change rien. Et contrairement à l'encaissement, IL N'Y A
     * AUCUN REPLI : un versement part du solde marchand, il n'existe pas de
     * page où le client irait se payer lui-même.
     *
     * Le drapeau évite donc un aller-retour coûteux et trompeur : sans lui, un
     * retrait débite le portefeuille, appelle le prestataire, se fait refuser,
     * puis recrédite — le solde de l'utilisateur fait un aller-retour visible
     * et le journal se remplit d'erreurs pour une fonction simplement pas
     * encore ouverte.
     */
    private boolean payoutsEnabled = false;

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

    /** Vrai quand un versement sortant peut réellement être tenté. */
    public boolean isPayoutUsable() {
        return isUsable() && payoutsEnabled;
    }

    /**
     * Contrôle de cohérence au démarrage.
     *
     * ═══ POURQUOI AU DÉMARRAGE ET NON À L'USAGE ═══
     *
     * Une clé publique (`pk_...`) à la place de la clé secrète est l'erreur de
     * configuration la plus facile à commettre : les deux se ressemblent, se
     * copient au même endroit du tableau de bord, et seule la seconde ouvre
     * l'API serveur. Sans ce contrôle, la faute ne se manifeste qu'au premier
     * dépôt d'un utilisateur, sous la forme d'un « prestataire indisponible »
     * qui envoie chercher le problème partout sauf dans le fichier .env.
     *
     * On avertit, on ne bloque pas : un serveur doit pouvoir démarrer avec une
     * configuration imparfaite — les autres fonctionnalités n'ont pas à en
     * souffrir.
     */
    @PostConstruct
    void verifierConfiguration() {
        if (!enabled) return;

        if (apiKey.isBlank()) {
            log.warn("FedaPay est activé mais aucune clé API n'est renseignée : "
                    + "seules les opérations de test fonctionneront.");
            return;
        }

        if (!apiKey.startsWith("sk_")) {
            log.error("FEDAPAY_API_KEY ne commence pas par « sk_ » : l'API serveur de FedaPay "
                    + "exige la clé SECRÈTE. Une clé publique (pk_...) sert au formulaire de "
                    + "paiement dans le navigateur et sera refusée en 401. Tout dépôt ou "
                    + "retrait réel échouera.");
        }

        boolean production = !baseUrl.contains("sandbox");
        if (production && apiKey.startsWith("sk_sandbox")) {
            log.error("Clé de BAC À SABLE pointée sur l'API de PRODUCTION ({}). "
                    + "Aucune opération n'aboutira.", baseUrl);
        }
        if (!production && apiKey.startsWith("sk_live")) {
            log.error("Clé de PRODUCTION pointée sur le bac à sable. "
                    + "Vérifiez FEDAPAY_BASE_URL avant d'encaisser quoi que ce soit.");
        }
    }
}
