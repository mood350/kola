package com.kola.backend.exception;

import org.springframework.http.HttpStatus;

/** The operation is within the balance but over a KYC-tier limit (KOLA.md 4.4). */
public class LimitExceededException extends ApiException {

    public LimitExceededException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "LIMIT_EXCEEDED", message);
    }
}
