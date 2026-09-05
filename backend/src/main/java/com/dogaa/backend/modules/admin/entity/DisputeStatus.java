package com.dogaa.backend.modules.admin.entity;

/** Where a claim stands (BACKEND.md 9). Wire values are the lower-case names the console styles on. */
public enum DisputeStatus {

    /** Filed, nothing decided. */
    OPEN("open"),
    /** A chargeback was started and is collecting its validations. */
    CHARGEBACK_PENDING("chargeback_pending"),
    /** Validated by enough admins; the funds have been reversed. */
    RESOLVED("resolved"),
    /** Closed without action. */
    REJECTED("rejected");

    private final String wireValue;

    DisputeStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
