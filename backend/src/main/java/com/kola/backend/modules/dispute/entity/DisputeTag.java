package com.kola.backend.modules.dispute.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Why a transaction is contested. The codes are part of the admin contract (BACKEND.md 9). */
public enum DisputeTag {

    FRAUD("fraud", "Fraude"),
    DOUBLE_DEBIT("double_debit", "Double débit"),
    P2P("p2p", "Transfert P2P");

    private final String code;
    private final String label;

    DisputeTag(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static DisputeTag fromCode(String value) {
        for (DisputeTag tag : values()) {
            if (tag.code.equalsIgnoreCase(value) || tag.name().equalsIgnoreCase(value)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("Motif de litige inconnu : " + value);
    }
}
