package com.kola.backend.exception;

/**
 * Lancée quand une opération dépasse la limite autorisée pour le niveau
 * KYC actuel de l'utilisateur (ex: TIER_0 ne peut pas envoyer plus de
 * 50 000 XOF/jour). L'utilisateur doit soumettre une pièce d'identité
 * pour monter de niveau et débloquer des limites plus élevées.
 */
public class KycLimitExceededException extends RuntimeException {
    public KycLimitExceededException(String message) {
        super(message);
    }

    public KycLimitExceededException() {
        super("Cette opération dépasse la limite autorisée pour votre niveau de vérification.");
    }
}
