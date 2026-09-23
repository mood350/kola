package com.kola.backend.payment;

import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionResponse;
import com.kola.backend.transaction.TransactionService;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestration d'un retrait vers Mobile Money.
 *
 * Même découpage que le dépôt, pour la même raison : l'appel au prestataire
 * reste HORS de la transaction base de données. Tenir une connexion ouverte
 * pendant l'échange avec l'opérateur épuiserait le pool sous charge, et les
 * verrous posés sur le portefeuille bloqueraient toutes ses autres opérations
 * le temps du réseau.
 *
 * ═══ CE QUI DIFFÈRE DU DÉPÔT ═══
 *
 * Le portefeuille est DÉJÀ DÉBITÉ quand on appelle le prestataire. Un échec ici
 * n'est donc pas neutre : il laisse un client amputé d'un argent qui n'est parti
 * nulle part. Le rattrapage n'est pas une commodité, c'est l'invariant central
 * de cette classe — d'où le recrédit systématique en cas de refus.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileMoneyWithdrawalService {

    private static final String PROVIDER = "FEDAPAY";

    /**
     * Préfixe des identifiants de versement.
     *
     * `provider_transaction_id` est unique et partagé par les encaissements et
     * les versements. Rien ne garantit que FedaPay ne réutilise pas un même
     * nombre pour une transaction et un payout — ce sont deux séquences
     * distinctes chez eux. Sans préfixe, la collision serait rare mais
     * possible, et se manifesterait par un retrait refusé en base sans raison
     * compréhensible.
     */
    private static final String PAYOUT_PREFIX = "payout_";

    private final TransactionService transactionService;
    private final PaymentProvider paymentProvider;

    /**
     * Débite le portefeuille et lance le versement.
     *
     * Le retour est une écriture EN ATTENTE : l'argent a quitté le portefeuille
     * mais n'est pas encore arrivé chez l'opérateur. L'interface doit dire
     * « retrait en cours », jamais « retrait effectué ».
     */
    public TransactionResponse withdraw(User currentUser, MobileMoneyWithdrawalRequest request) {
        /* ═══ REFUSER AVANT D'OUVRIR L'ÉCRITURE ═══
           Le versement est une autorisation accordée compte par compte : quand
           elle manque, l'appel est refusé quoi qu'on envoie. Le demander ici
           épargne à l'utilisateur un solde qui descend puis remonte, et au
           journal une erreur par tentative pour une fonction simplement pas
           encore ouverte. C'est aussi la seule place où le refus ne coûte
           rien : après `openMobileMoneyWithdrawal`, le portefeuille est
           débité. */
        if (!paymentProvider.payoutsAvailable()) {
            throw new PaymentMethodUnavailableException(
                    "Le retrait vers Mobile Money n'est pas encore disponible. "
                            + "Vos autres opérations restent accessibles.");
        }

        Transaction pending = transactionService.openMobileMoneyWithdrawal(currentUser, request);

        /* Rejeu d'une clé déjà utilisée : le versement est déjà parti. On rend
           l'écriture telle quelle, sans redemander un second versement. */
        if (pending.getProviderTransactionId() != null
                || pending.getStatus() != TransactionStatus.PENDING) {
            return TransactionResponse.fromEntity(pending);
        }

        PaymentProvider.PayoutInitiation initiation;
        try {
            initiation = paymentProvider.initiatePayout(new PaymentProvider.PayoutCommand(
                    pending.getAmount(),
                    pending.getCurrency(),
                    request.mode(),
                    request.phoneNumber(),
                    request.mode().getCountryCode(),
                    pending.getReference(),
                    "Retrait Kola " + pending.getReference(),
                    currentUser.getEmail(),
                    currentUser.getFirstName(),
                    currentUser.getLastName()
            ));
        } catch (PaymentProviderException failure) {
            /* Le prestataire n'a rien pris en charge : le portefeuille a été
               débité pour rien. On rend le montant ET les frais avant de
               propager l'erreur — c'est le seul moment où on peut le faire en
               connaissant l'état réel. */
            transactionService.abandonPendingWithdrawal(pending.getId());
            log.warn("Retrait {} abandonné, portefeuille recrédité : {}",
                    pending.getReference(), failure.getMessage());
            throw failure;
        }

        /* Aucune URL : un versement part du solde marchand, le client n'a rien
           à valider. */
        transactionService.attachProviderTransaction(
                pending.getId(), PROVIDER, PAYOUT_PREFIX + initiation.providerPayoutId(), null);

        return transactionService.getByReference(currentUser, pending.getReference());
    }

    /** Retire le préfixe pour retrouver l'identifiant attendu par le prestataire. */
    public static String toProviderPayoutId(String providerTransactionId) {
        return providerTransactionId.startsWith(PAYOUT_PREFIX)
                ? providerTransactionId.substring(PAYOUT_PREFIX.length())
                : providerTransactionId;
    }
}
