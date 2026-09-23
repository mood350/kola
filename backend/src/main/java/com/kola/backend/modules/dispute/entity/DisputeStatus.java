package com.kola.backend.modules.dispute.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DisputeStatus {

    /** Reported, nothing decided. */
    OPEN("open"),
    /** A chargeback was proposed and is collecting validations. */
    CHARGEBACK_PENDING("chargeback_pending"),
    /** Validated by enough administrators; the money has been moved back. */
    RESOLVED("resolved"),
    /** Closed without action. */
    REJECTED("rejected");

    private final String code;

    DisputeStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public boolean isClosed() {
        return this == RESOLVED || this == REJECTED;
    }
}
