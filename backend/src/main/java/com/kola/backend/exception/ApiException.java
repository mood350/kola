package com.kola.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base for every error the API deliberately returns to a client. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
