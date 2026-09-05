package com.dogaa.backend.modules.admin.entity;

/**
 * Why a transaction is contested (BACKEND.md 9). The wire value is the lower-case name the admin
 * console styles on; the label is the French text it prints next to it.
 */
public enum DisputeTag {

    FRAUD("fraud", "Fraude suspectée"),
    DOUBLE_DEBIT("double_debit", "Double débit"),
    P2P("p2p", "Litige P2P");

    private final String wireValue;
    private final String label;

    DisputeTag(String wireValue, String label) {
        this.wireValue = wireValue;
        this.label = label;
    }

    public String wireValue() {
        return wireValue;
    }

    public String label() {
        return label;
    }
}
