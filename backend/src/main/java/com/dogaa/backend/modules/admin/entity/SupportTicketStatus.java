package com.dogaa.backend.modules.admin.entity;

/** Where a customer ticket stands (BACKEND.md 13). */
public enum SupportTicketStatus {

    OPEN("open"),
    IN_PROGRESS("in_progress"),
    RESOLVED("resolved");

    private final String wireValue;

    SupportTicketStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }
}
