package com.dogaa.backend.common.enums;

/**
 * Currencies a wallet can hold (DOGAA.md 4.1). {@link #getDecimalPlaces()} is how many
 * minor-unit digits the currency actually uses — XOF is quoted in whole francs, the rest
 * in cents — so amounts and fees can be rounded to something the currency can represent.
 */
public enum Currency {
    XOF(0),
    GHS(2),
    NGN(2),
    USD(2);

    private final int decimalPlaces;

    Currency(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}
