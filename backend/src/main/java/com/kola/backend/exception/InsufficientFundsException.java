package com.kola.backend.exception;

/**
 * Lancée quand le solde du wallet est insuffisant pour une transaction.
 * Ex : tentative d'envoi de 50 000 XOF avec seulement 30 000 XOF disponibles.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException() {
        super("Solde insuffisant pour effectuer cette opération");
    }
}