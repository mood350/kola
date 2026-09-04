package com.dogaa.backend.modules.auth.security;

import com.dogaa.backend.exception.BadRequestException;

/**
 * A 4-digit PIN has only 10 000 combinations, and a large share of real users pick
 * "1234" or their birth year. Rejecting the trivial ones costs nothing and removes
 * the cheapest attack on a wallet.
 */
public final class PinPolicy {

    private PinPolicy() {
    }

    public static void validate(String pin) {
        if (pin == null || !pin.matches("[0-9]{4,6}")) {
            throw new BadRequestException("PIN must be 4 to 6 digits");
        }
        if (isAllSameDigit(pin)) {
            throw new BadRequestException("PIN must not repeat the same digit");
        }
        if (isSequential(pin)) {
            throw new BadRequestException("PIN must not be a sequence of consecutive digits");
        }
    }

    private static boolean isAllSameDigit(String pin) {
        return pin.chars().distinct().count() == 1;
    }

    private static boolean isSequential(String pin) {
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < pin.length(); i++) {
            int delta = pin.charAt(i) - pin.charAt(i - 1);
            ascending &= delta == 1;
            descending &= delta == -1;
        }
        return ascending || descending;
    }
}
