package com.kola.backend.modules.admin.entity;

import com.kola.backend.exception.BadRequestException;

/** Where a partner merchant stands (BACKEND.md 10). Wire values are the console's lower-case names. */
public enum MerchantStatus {

    /** Applied, not yet accepted. Cannot take payments. */
    PENDING("pending"),
    /** Accepted and taking payments. */
    ACTIVE("active"),
    /** Switched off — by us or at their request. */
    SUSPENDED("suspended");

    private final String wireValue;

    MerchantStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    /**
     * The console computes the next status itself and sends it, so it is re-checked here: a client
     * is free to send anything, and "approve a suspended merchant" is not a transition the product
     * has ever offered.
     */
    public boolean canMoveTo(MerchantStatus target) {
        return switch (this) {
            case PENDING -> target == ACTIVE || target == SUSPENDED;
            case ACTIVE -> target == SUSPENDED;
            case SUSPENDED -> target == ACTIVE;
        };
    }

    public static MerchantStatus fromWireValue(String value) {
        for (MerchantStatus status : values()) {
            if (status.wireValue.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new BadRequestException("Statut marchand inconnu : « " + value + " »");
    }
}
