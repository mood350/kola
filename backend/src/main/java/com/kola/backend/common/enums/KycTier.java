package com.kola.backend.common.enums;

/**
 * Progressive KYC levels (KOLA.md 4.4). Each tier raises the transaction limits;
 * TIER_2 is the gate for credit.
 */
public enum KycTier {
    TIER_0,
    TIER_1,
    TIER_2,
    TIER_3;

    public boolean isAtLeast(KycTier required) {
        return this.ordinal() >= required.ordinal();
    }
}
