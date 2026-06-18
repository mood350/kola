package com.kola.backend.exception;

/**
 * Lancée quand l'utilisateur tente de créer un wallet ou d'effectuer une
 * opération dans une devise non supportée par Kola.
 */
public class UnsupportedCurrencyException extends RuntimeException {
    public UnsupportedCurrencyException(String message) {
        super(message);
    }

    public UnsupportedCurrencyException() {
        super("Cette devise n'est pas supportée par Kola.");
    }
}
