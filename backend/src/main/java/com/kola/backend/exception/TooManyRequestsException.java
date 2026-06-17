package com.kola.backend.exception;

/**
 * Lancée quand le rate limiting est déclenché.
 * HTTP 429 TOO MANY REQUESTS.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }

    public TooManyRequestsException() {
        super("Trop de tentatives. Veuillez réessayer dans quelques minutes.");
    }
}