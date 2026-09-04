package com.dogaa.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success,
                            String code,
                            String message,
                            Map<String, String> fieldErrors,
                            String path,
                            Instant timestamp) {

    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(false, code, message, null, path, Instant.now());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fieldErrors, String path) {
        return new ErrorResponse(false, code, message, fieldErrors, path, Instant.now());
    }
}
