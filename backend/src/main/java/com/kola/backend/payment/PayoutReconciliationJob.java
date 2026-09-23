package com.kola.backend.payment;

import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Relit l'issue des retraits partis chez le prestataire.
 *
 * ═══ POURQUOI CE TRAVAIL EXISTE ALORS QUE LES DÉPÔTS ONT UN WEBHOOK ═══
 *
 * Parce que FedaPay n'en envoie pas pour les versements. Sa documentation ne
 * liste d'événements que pour les transactions et les clients : un retrait
 * change d'état chez eux sans que personne ne nous prévienne. Sans ce
 * rapprochement, une écriture partie en attente y resterait indéfiniment — et
 * un retrait échoué ne serait jamais recrédité.
 *
 * ═══ CE QU'IL NE FAIT PAS ═══
 *
 * Il ne conclut jamais à partir d'un silence. Un versement encore « pending »
 * ou « processing » est laissé en attente ; seuls « sent » et « failed » sont
 * des verdicts. Déclarer un échec sur un délai recréditerait un portefeuille
 * dont l'argent est peut-être déjà parti — l'erreur la plus coûteuse possible
 * ici, puisqu'elle crée de l'argent.
 *
 * Toutes les cinq minutes : assez souvent pour qu'un client ne reste pas dans
 * le flou, assez rare pour ne pas marteler l'API du prestataire.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutReconciliationJob {

    private final TransactionService transactionService;
    private final PaymentProvider paymentProvider;

    @Scheduled(fixedDelayString = "PT5M")
    public void reconcilePendingPayouts() {
        /* Rien à rapprocher si les versements ne sont pas ouverts : aucun
           n'a pu partir. La question est posée au prestataire et non à sa
           configuration, pour que ce travail reste vrai le jour où il y en a
           un second. */
        if (!paymentProvider.payoutsAvailable()) return;

        List<Transaction> pending = transactionService.findPendingProviderWithdrawals();
        if (pending.isEmpty()) return;

        log.info("Rapprochement de {} retrait(s) en attente", pending.size());

        for (Transaction tx : pending) {
            try {
                String payoutId = MobileMoneyWithdrawalService
                        .toProviderPayoutId(tx.getProviderTransactionId());

                PaymentProvider.PayoutStatus status = paymentProvider.payoutStatus(payoutId);

                switch (status) {
                    case SENT -> transactionService
                            .settleMobileMoneyWithdrawal(tx.getProviderTransactionId(), true);
                    case FAILED -> transactionService
                            .settleMobileMoneyWithdrawal(tx.getProviderTransactionId(), false);
                    /* En cours : on repassera. Ne rien faire est ici la bonne
                       action, pas une omission. */
                    case PENDING -> { }
                }
            } catch (Exception e) {
                /* Un retrait illisible ne doit pas empêcher de rapprocher les
                   suivants : la boucle continue, l'incident est journalisé. */
                log.error("Rapprochement impossible pour le retrait {}", tx.getReference(), e);
            }
        }
    }
}
