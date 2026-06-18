package com.kola.backend.exception;

public class ActiveLoanExistsException extends RuntimeException {
    public ActiveLoanExistsException() {
        super("Vous avez déjà un prêt en cours. Remboursez-le avant d'en demander un nouveau.");
    }
}
