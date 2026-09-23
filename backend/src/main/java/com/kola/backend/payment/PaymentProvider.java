package com.kola.backend.payment;

import java.math.BigDecimal;

/**
 * Prestataire d'encaissement Mobile Money.
 *
 * POURQUOI UNE INTERFACE POUR UN SEUL PRESTATAIRE : parce que le reste du code
 * ne doit rien savoir de FedaPay. {@code TransactionService} orchestre un
 * dépôt — il crée l'écriture au grand livre, applique le plafond KYC, notifie —
 * et délègue ici le seul geste qui sorte du système. Le jour où un second
 * agrégateur s'ajoute (ou remplace celui-ci), c'est une implémentation de plus,
 * pas une réécriture du service qui tient l'argent.
 *
 * LE PÉRIMÈTRE EST VOLONTAIREMENT ÉTROIT : l'encaissement, et rien d'autre. Les
 * versements sortants (retraits) restent à faire ; ils ajouteront une méthode
 * ici plutôt qu'une seconde abstraction concurrente.
 */
public interface PaymentProvider {

    /**
     * Demande à l'opérateur de débiter le compte Mobile Money du client.
     *
     * L'appel N'EST PAS L'ENCAISSEMENT : il déclenche la demande, l'utilisateur
     * doit encore valider sur son téléphone. Le résultat n'est donc jamais un
     * succès, seulement une prise en charge — c'est le webhook qui dira si
     * l'argent est arrivé.
     *
     * @throws PaymentProviderException si le prestataire refuse la demande ou
     *         reste injoignable. L'appelant doit alors considérer qu'AUCUN
     *         mouvement n'a eu lieu.
     */
    CollectInitiation initiateCollect(CollectCommand command);

    /**
     * Verse de l'argent depuis le solde marchand vers le compte du client.
     *
     * COMME POUR L'ENCAISSEMENT, L'APPEL N'EST PAS LE VERSEMENT : il ouvre puis
     * démarre l'ordre chez le prestataire, qui le traite ensuite de façon
     * asynchrone. Le résultat n'est donc jamais « payé », seulement « pris en
     * charge ».
     *
     * @throws PaymentProviderException si l'ordre n'a pas pu être ouvert. Dans
     *         ce cas AUCUN argent n'a quitté le solde marchand, et l'appelant
     *         doit recréditer le portefeuille qu'il avait débité.
     */
    PayoutInitiation initiatePayout(PayoutCommand command);

    /**
     * Statut courant d'un versement chez le prestataire.
     *
     * NÉCESSAIRE PARCE QU'AUCUN WEBHOOK NE COUVRE LES VERSEMENTS : la
     * documentation FedaPay ne liste d'événements que pour les transactions et
     * les clients. Un retrait ne peut donc pas être notifié — il doit être
     * relu, ce que fait le travail de réconciliation.
     */
    PayoutStatus payoutStatus(String providerPayoutId);

    /**
     * Ce prestataire peut-il verser, ici et maintenant ?
     *
     * POSÉE AVANT DE DÉBITER QUOI QUE CE SOIT. Chez FedaPay, les versements
     * sont une autorisation accordée compte par compte : sans elle, l'appel
     * revient en 403 quoi qu'on envoie. Débiter un portefeuille pour découvrir
     * ça, puis le recréditer, fait faire au solde un aller-retour que
     * l'utilisateur voit — et qu'il ne comprend pas.
     *
     * Sur l'interface plutôt que sur les propriétés FedaPay pour que le service
     * de retrait reste ignorant du prestataire, comme le reste du code.
     */
    boolean payoutsAvailable();

    /**
     * Ce qu'il faut savoir pour déclencher un encaissement.
     *
     * @param amount        montant en devise pleine (le franc CFA n'a pas de
     *                      subdivision, cf. {@code FedaPayClient})
     * @param currency      code ISO 4217, « XOF »
     * @param mode          opérateur à débiter
     * @param phoneNumber   numéro au format international, ex. +22890000000
     * @param countryCode   ISO 3166-1 alpha-2 du numéro
     * @param reference     référence Kola de l'écriture (KLA-…), transmise au
     *                      prestataire comme référence marchand : c'est elle qui
     *                      relie les deux grands livres lors d'un litige
     * @param description   libellé lisible par le client sur son relevé
     * @param customerEmail identifie le client chez le prestataire
     */
    record CollectCommand(
            BigDecimal amount,
            String currency,
            MobileMoneyMode mode,
            String phoneNumber,
            String countryCode,
            String reference,
            String description,
            String customerEmail,
            String customerFirstName,
            String customerLastName
    ) {}

    /**
     * Prise en charge par le prestataire.
     *
     * @param providerTransactionId identifiant chez le prestataire — la clé par
     *                              laquelle le webhook retrouvera l'écriture
     * @param status                statut brut renvoyé (« pending » en général)
     * @param paymentUrl            page où le client règle, quand le débit n'a
     *                              pas pu être poussé sur son téléphone ; nulle
     *                              en prélèvement direct, où il n'y a rien à
     *                              ouvrir
     */
    record CollectInitiation(String providerTransactionId, String status, String paymentUrl) {}

    /**
     * Ordre de versement.
     *
     * @param amount      montant net versé au client
     * @param currency    code ISO 4217
     * @param mode        opérateur destinataire
     * @param phoneNumber numéro qui recevra l'argent, format international
     * @param countryCode ISO 3166-1 alpha-2 du numéro
     * @param reference   référence Kola de l'écriture, transmise au prestataire
     */
    record PayoutCommand(
            BigDecimal amount,
            String currency,
            MobileMoneyMode mode,
            String phoneNumber,
            String countryCode,
            String reference,
            String description,
            String customerEmail,
            String customerFirstName,
            String customerLastName
    ) {}

    /** Prise en charge d'un versement. */
    record PayoutInitiation(String providerPayoutId, String status) {}

    /**
     * Issue d'un versement, telle que la lit la réconciliation.
     *
     * Les statuts bruts de FedaPay (pending, started, processing, sent, failed)
     * sont ramenés à trois cas, parce que le grand livre n'en distingue pas
     * davantage : soit l'argent est parti, soit il ne partira pas, soit on ne
     * sait pas encore.
     */
    enum PayoutStatus {
        PENDING,
        SENT,
        FAILED
    }
}
