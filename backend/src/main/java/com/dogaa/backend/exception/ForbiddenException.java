package com.dogaa.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * The caller is authenticated but not allowed to do this.
 *
 * <p>Distinct from a 401 on purpose: the admin front-end treats every 401 as an expired session and
 * signs the user out, so a refused permission must never come back as one (BACKEND.md 3).
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
