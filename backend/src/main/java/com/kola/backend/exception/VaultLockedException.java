package com.kola.backend.exception;

/**
 * Lancée quand l'utilisateur tente de retirer des fonds
 * d'un coffre-fort encore verrouillé (unlockDate pas encore atteinte).
 * HTTP 423 LOCKED.
 */
public class VaultLockedException extends RuntimeException {
    public VaultLockedException(String message) {
        super(message);
    }

    public VaultLockedException() {
        super("Ce coffre-fort est verrouillé et ne peut pas être débloqué avant la date prévue");
    }
}