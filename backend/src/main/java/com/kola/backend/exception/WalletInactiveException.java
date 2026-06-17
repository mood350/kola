package com.kola.backend.exception;

/**
 * Lancée quand une opération est tentée sur un wallet
 * suspendu par l'administrateur (active = false).
 */
public class WalletInactiveException extends RuntimeException {
    public WalletInactiveException(String message) {
        super(message);
    }

    public WalletInactiveException() {
        super("Ce portefeuille est suspendu. Veuillez contacter le support.");
    }
}