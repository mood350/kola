package com.kola.backend.exception;

public class ActiveLoanExistsException extends RuntimeException {
    public ActiveLoanExistsException() {
        super("Vous avez déjà un prêt en cours. Remboursez-le avant d'en demander un nouveau.");
    }

    public ActiveLoanExistsException(String message) {
        super(message);
    }

    /** Prêt échu et non remboursé : le message doit dire quoi faire. */
    public static ActiveLoanExistsException defaulted() {
        return new ActiveLoanExistsException(
                "Vous avez un prêt en défaut de paiement. Régularisez-le avant d'en demander un nouveau.");
    }
}
