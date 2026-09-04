package com.dogaa.backend.common.enums;

/**
 * Progressive verification tiers (spec DOGAA.md §4.4). Each tier raises
 * transaction limits; TIER_2 is the gate for credit eligibility.
 */
public enum KycTier {
    TIER_0,
    TIER_1,
    TIER_2,
    TIER_3
}
