package com.kola.backend.payment;

import com.kola.backend.transaction.MobileMoneyDepositRequest;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionResponse;
import com.kola.backend.transaction.TransactionService;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestration d'un rechargement Mobile Money.
 *
 * ═══ POURQUOI CETTE CLASSE EXISTE PLUTÔT QU'UNE MÉTHODE DE PLUS ═══
 *
 * Pour tenir l'appel réseau HORS de la transaction base de données. Ouvrir
 * l'écriture, appeler FedaPay et enregistrer le résultat dans une seule méthode
 * {@code @Transactional} garderait la transaction — et ses verrous — ouverte
 * pendant toute la durée de l'échange avec l'opérateur. Sous charge, quelques
 * secondes d'attente réseau multipliées par le nombre de dépôts simultanés
 * épuisent le pool de connexions, et c'est toute l'application qui s'arrête,
 * pas seulement les dépôts.
 *
 * Ici, chaque étape qui touche la base est une transaction courte et distincte,
 * et l'appel réseau se produit entre les deux, à découvert. Le passage par un
 * bean séparé n'est pas cosmétique : appeler {@code this.methodeTransactionnelle()}
 * dans la même classe court-circuite le proxy Spring et n'ouvrirait aucune
 * transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileMoneyDepositService {

    private static final String PROVIDER = "FEDAPAY";

    private final TransactionService transactionService;
    private final PaymentProvider paymentProvider;

    /**
     * Déclenche une demande de débit sur le téléphone du client.
     *
     * Le retour est une écriture EN ATTENTE — jamais un succès. Le client doit
     * encore composer son code Mobile Money, et c'est le webhook qui créditera.
     * L'interface doit donc afficher « demande envoyée », pas « dépôt effectué ».
     */
    public TransactionResponse deposit(User currentUser, MobileMoneyDepositRequest request) {
        Transaction pending = transactionService.openMobileMoneyDeposit(currentUser, request);

        /* Rejeu d'une clé d'idempotence déjà utilisée : l'écriture existe et sa
           demande est déjà partie chez l'opérateur. La renvoyer telle quelle,
           sans repartir vers FedaPay — sinon le client verrait deux demandes de
           débit s'afficher sur son téléphone pour un seul dépôt voulu. */
        if (pending.getProviderTransactionId() != null
                || pending.getStatus() != TransactionStatus.PENDING) {
            return TransactionResponse.fromEntity(pending);
        }

        PaymentProvider.CollectInitiation initiation;
        try {
            initiation = paymentProvider.initiateCollect(new PaymentProvider.CollectCommand(
                    pending.getAmount(),
                    pending.getCurrency(),
                    request.mode(),
                    request.phoneNumber(),
                    request.mode().getCountryCode(),
                    pending.getReference(),
                    "Rechargement Kola " + pending.getReference(),
                    currentUser.getEmail(),
                    currentUser.getFirstName(),
                    currentUser.getLastName()
            ));
        } catch (PaymentProviderException failure) {
            /* Le prestataire n'a rien pris en charge : aucun franc n'a bougé
               nulle part. On referme l'écriture pour ne pas laisser au grand
               livre une ligne en attente qui n'aboutira jamais, et l'erreur
               remonte telle quelle à l'appelant. */
            transactionService.abandonPendingDeposit(pending.getId());
            log.warn("Rechargement {} abandonné : {}", pending.getReference(), failure.getMessage());
            throw failure;
        }

        transactionService.attachProviderTransaction(
                pending.getId(), PROVIDER, initiation.providerTransactionId());

        /* Relecture plutôt que retour de l'objet en mémoire : celui-ci a été
           chargé avant que l'identifiant du prestataire ne soit posé, et le
           client a besoin de la version enregistrée. */
        return transactionService.getByReference(currentUser, pending.getReference());
    }
}
