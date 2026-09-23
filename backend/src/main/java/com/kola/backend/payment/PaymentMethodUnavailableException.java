package com.kola.backend.payment;

/**
 * Un moyen de paiement que ce serveur ne peut pas offrir aujourd'hui.
 *
 * ═══ CE N'EST PAS UNE PANNE, ET C'EST TOUT L'INTÉRÊT ═══
 *
 * {@link PaymentProviderException} dit « on a essayé et ça s'est mal passé » :
 * elle sort en 502 et journalise une erreur, parce qu'un appel a échoué.
 * Celle-ci dit « on n'essaiera pas » — le canal n'est pas ouvert sur ce compte
 * marchand, la tentative serait refusée d'avance. Les confondre remplirait le
 * journal d'erreurs sur un fonctionnement nominal, et enverrait le support
 * chercher une panne qui n'existe pas.
 *
 * Conséquence pratique : l'opération est refusée AVANT le moindre mouvement au
 * grand livre. Rien n'est débité, rien n'est à recréditer.
 */
public class PaymentMethodUnavailableException extends RuntimeException {

    public PaymentMethodUnavailableException(String message) {
        super(message);
    }
}
