package com.kola.backend.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implémentation FedaPay de {@link PaymentProvider}.
 *
 * Elle enchaîne les trois appels du client HTTP et ne retient que ce dont le
 * grand livre a besoin : l'identifiant de l'opération chez le prestataire.
 *
 * AUCUN RATTRAPAGE ICI. Si l'un des trois appels échoue, l'exception remonte
 * telle quelle et le service appelant annule son écriture. Réessayer
 * automatiquement au milieu d'une séquence de paiement est le meilleur moyen de
 * créer deux demandes de débit pour un seul dépôt voulu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FedaPayPaymentProvider implements PaymentProvider {

    private final FedaPayClient client;

    @Override
    public CollectInitiation initiateCollect(CollectCommand command) {
        String transactionId = client.createTransaction(command);
        String token = client.generateToken(transactionId);
        String status = client.sendNow(
                token,
                command.mode(),
                command.phoneNumber(),
                command.countryCode()
        );

        log.info("Encaissement FedaPay initié : transaction {} (réf. Kola {}) — statut {}",
                transactionId, command.reference(), status);

        return new CollectInitiation(transactionId, status);
    }
}
