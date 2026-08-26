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
     */
    record CollectInitiation(String providerTransactionId, String status) {}
}
