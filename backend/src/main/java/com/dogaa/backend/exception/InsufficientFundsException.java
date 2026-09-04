package com.dogaa.backend.exception;

import org.springframework.http.HttpStatus;

/** A wallet does not hold enough available (or locked) balance for the requested move. */
public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "INSUFFICIENT_FUNDS", message);
    }
}
