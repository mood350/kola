package com.kola.backend.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implémentation FedaPay de {@link PaymentProvider}.
 *
 * Elle enchaîne les appels du client HTTP et ne retient que ce dont le grand
 * livre a besoin : l'identifiant de l'opération chez le prestataire, et — quand
 * le paiement se fait sur une page hébergée — le lien qui permet de le régler.
 *
 * ═══ DEUX CHEMINS D'ENCAISSEMENT, UN SEUL DÉNOUEMENT ═══
 *
 * Prélèvement direct ou page hébergée, la différence tient à QUI présente la
 * demande au client : l'opérateur sur son téléphone, ou FedaPay dans son
 * navigateur. Après quoi les deux convergent exactement : même transaction chez
 * le prestataire, même webhook {@code transaction.approved}, même règlement.
 * Rien en aval d'ici ne distingue les deux, et c'est voulu — un second chemin
 * de crédit serait un second endroit où se tromper sur de l'argent.
 *
 * AUCUN RATTRAPAGE ICI. Si un appel échoue, l'exception remonte telle quelle et
 * le service appelant annule son écriture. Réessayer automatiquement au milieu
 * d'une séquence de paiement est le meilleur moyen de créer deux demandes de
 * débit pour un seul dépôt voulu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FedaPayPaymentProvider implements PaymentProvider {

    private final FedaPayClient client;
    private final FedaPayProperties properties;

    @Override
    public CollectInitiation initiateCollect(CollectCommand command) {
        FedaPayClient.CreatedTransaction created = client.createTransaction(command);

        return properties.isDirectCharge()
                ? pushToPhone(created.id(), command)
                : hostedCheckout(created, command);
    }

    /**
     * Prélèvement sans redirection : la demande s'affiche sur le téléphone.
     *
     * Suppose que FedaPay a ouvert ce canal sur le compte marchand. Sinon
     * l'appel revient en 400 « Opération non autorisée » — d'où le drapeau qui
     * garde ce chemin fermé par défaut.
     */
    private CollectInitiation pushToPhone(String transactionId, CollectCommand command) {
        FedaPayClient.PaymentToken token = client.generateToken(transactionId);
        String status = client.sendNow(
                token.token(),
                command.mode(),
                command.phoneNumber(),
                command.countryCode()
        );

        log.info("Encaissement FedaPay initié (prélèvement direct) : transaction {} "
                + "(réf. Kola {}) — statut {}", transactionId, command.reference(), status);

        return new CollectInitiation(transactionId, status, null);
    }

    /**
     * Page de paiement hébergée : le client valide chez FedaPay.
     *
     * L'URL accompagne déjà la création de la transaction. Le repli par le
     * jeton n'est pas de la superstition : les deux documentations en ligne ne
     * s'accordent pas sur la présence de {@code payment_url} à la création, et
     * l'endpoint du jeton rend le même lien. Mieux vaut un appel de plus qu'un
     * dépôt impayable.
     *
     * Sans URL au bout des deux tentatives on LÈVE : rendre une écriture en
     * attente que personne ne peut régler laisserait le client devant un dépôt
     * qui n'aboutira jamais, sans rien lui dire.
     */
    private CollectInitiation hostedCheckout(FedaPayClient.CreatedTransaction created,
                                             CollectCommand command) {
        String url = created.paymentUrl();
        if (url == null) {
            url = client.generateToken(created.id()).url();
        }
        if (url == null) {
            throw new PaymentProviderException(
                    "Réponse FedaPay inexploitable : aucune page de paiement.");
        }

        log.info("Encaissement FedaPay initié (page hébergée) : transaction {} (réf. Kola {})",
                created.id(), command.reference());

        return new CollectInitiation(created.id(), "pending", url);
    }

    @Override
    public PayoutInitiation initiatePayout(PayoutCommand command) {
        String payoutId = client.createPayout(command);

        /* Créer puis démarrer : si le démarrage échoue, l'ordre reste « pending »
           chez FedaPay et n'a rien versé. L'exception remonte, l'appelant
           recrédite le portefeuille, et l'ordre dormant sera vu en
           rapprochement — jamais un retrait fantôme. */
        client.startPayout(payoutId);

        log.info("Versement FedaPay démarré : payout {} (réf. Kola {})",
                payoutId, command.reference());

        return new PayoutInitiation(payoutId, "started");
    }

    @Override
    public PayoutStatus payoutStatus(String providerPayoutId) {
        return client.payoutStatus(providerPayoutId);
    }

    @Override
    public boolean payoutsAvailable() {
        return properties.isPayoutUsable();
    }
}
