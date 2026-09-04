package com.dogaa.backend.common.util;

/**
 * Phone numbers are the login identifier, so they must be stored in exactly one shape.
 * Everything is normalised to E.164 ({@code +228XXXXXXXX}) before it touches the database:
 * users type "90 12 34 56", "0090123456" or "+228 90123456" for the same account.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * @param raw                what the user typed
     * @param defaultCallingCode calling code assumed for local-format numbers, e.g. "228"
     * @return the number in E.164 form
     * @throws IllegalArgumentException if the input cannot be read as a phone number
     */
    public static String normalize(String raw, String defaultCallingCode) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }
        if (!digits.startsWith("+")) {
            digits = "+" + defaultCallingCode + digits.replaceFirst("^0+", "");
        }
        String national = digits.substring(1);
        if (!national.matches("[0-9]{8,15}")) {
            throw new IllegalArgumentException("Invalid phone number: " + raw);
        }
        return "+" + national;
    }

    /** Masks all but the last two digits, for logs and notifications. */
    public static String mask(String e164) {
        if (e164 == null || e164.length() < 4) {
            return "***";
        }
        return e164.substring(0, 4) + "*".repeat(e164.length() - 6) + e164.substring(e164.length() - 2);
    }
}
