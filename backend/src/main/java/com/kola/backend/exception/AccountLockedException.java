package com.kola.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/** Raised when the PIN has been mistyped too many times and the account is cooling down. */
public class AccountLockedException extends ApiException {

    public AccountLockedException(Instant lockedUntil) {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Too many wrong PIN attempts. Try again after " + lockedUntil + ".");
    }
}
