package com.kola.backend.payment;

/**
 * Le prestataire de paiement a refusé la demande, ou n'a pas répondu.
 *
 * HTTP 502 BAD GATEWAY — et ce choix dit quelque chose d'utile : l'échec vient
 * d'un système tiers, pas de la requête du client ni d'un bug de Kola. Un 500
 * laisserait croire à une panne de notre côté ; un 400 accuserait l'utilisateur
 * d'une saisie incorrecte alors que son numéro était bon.
 *
 * INVARIANT QUE L'APPELANT PEUT TENIR POUR ACQUIS : quand cette exception est
 * levée, AUCUN mouvement d'argent n'a été enregistré. Le service de dépôt
 * annule donc son écriture en attente plutôt que de laisser au grand livre une
 * ligne qui n'aboutira jamais.
 */
public class PaymentProviderException extends RuntimeException {

    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
