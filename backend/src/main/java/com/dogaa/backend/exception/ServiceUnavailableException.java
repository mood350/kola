package com.dogaa.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * A dependency the feature needs is not configured or not answering.
 *
 * <p>503 rather than 500: the request was valid and retrying later may well work, which is exactly
 * what a client needs to know to decide between showing an error and hiding the feature.
 */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message);
    }
}
