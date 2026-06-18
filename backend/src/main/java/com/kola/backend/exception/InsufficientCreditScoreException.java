package com.kola.backend.exception;

public class InsufficientCreditScoreException extends RuntimeException {
    public InsufficientCreditScoreException(int score, int required) {
        super("Score de crédit insuffisant : " + score + "/100 (minimum requis : " + required + ").");
    }
}
