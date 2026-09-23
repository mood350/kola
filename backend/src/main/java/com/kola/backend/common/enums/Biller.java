package com.kola.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The billers a user can pay, and — the point of this class — <b>which identifier each one asks
 * for</b>. It is not the same question everywhere: Canal+ wants the card number under the decoder,
 * Cash Power wants the meter number, CEET and TdE want a customer reference, an insurer wants a
 * policy number. Asking for "votre numéro" and hoping is how a payment ends up somewhere else.
 *
 * <p><b>On validation.</b> Only Canal+ publishes a format — 14 digits, quoted in its own
 * re-subscription instructions. No other operator here publishes one. So the rules below are
 * deliberately loose: a character class and a length range, enough to catch a typed phone number or
 * an empty box, and nothing invented beyond that. Making up a strict pattern for a real company
 * would reject valid customers, and the failure would look like a Kola bug rather than a wrong
 * guess. The authoritative check belongs to the biller, whose rejection is surfaced as-is.
 *
 * <p><b>On fixed amounts.</b> {@code fixedAmount} says whether the bill is the same every month. A
 * subscription is (Canal+, CanalBox, fibre); consumption is not (electricity, water, prepaid
 * meters). Only the first kind can be scheduled for an automatic monthly payment — scheduling a
 * fixed sum against a variable bill would either underpay silently or overpay every month.
 *
 * <p>Kept as an enum rather than a table because it is reference data that changes when a
 * partnership is signed, not when a user acts. Move it to a table the day the back-office needs to
 * add a biller without a deployment.
 */
public enum Biller {

    // --- Subscriptions: the amount is the same every month --------------------

    CANAL_PLUS("canal_plus", "CANAL+", "Numéro de carte",
            IdentifierKind.DIGITS, 14, 14, true),

    CANALBOX("canalbox", "CANALBOX", "Numéro de carte",
            IdentifierKind.DIGITS, 6, 20, true),

    NEW_WORLD_TV("new_world_tv", "New World TV", "Numéro de carte",
            IdentifierKind.DIGITS, 6, 20, true),

    TOGOCOM_FIBRE("togocom_fibre", "Togocom — fibre optique", "Numéro de contrat",
            IdentifierKind.ALPHANUMERIC, 4, 24, true),

    MOOV_FIBRE("moov_fibre", "Moov Africa — fibre / Cizo-Bbox", "Numéro de contrat",
            IdentifierKind.ALPHANUMERIC, 4, 24, true),

    INSURANCE("insurance", "Assurance", "Numéro de police",
            IdentifierKind.ALPHANUMERIC, 4, 30, true),

    // --- Consumption: the amount changes every month --------------------------

    CEET("ceet", "CEET — électricité", "Référence client",
            IdentifierKind.ALPHANUMERIC, 4, 24, false),

    CASH_POWER("cash_power", "Cash Power — compteur prépayé", "Numéro du compteur",
            IdentifierKind.DIGITS, 6, 20, false),

    TDE("tde", "TdE — eau", "Référence client",
            IdentifierKind.ALPHANUMERIC, 4, 24, false);

    /** What a subscriber identifier may be made of. */
    public enum IdentifierKind {
        /** Digits only — a meter or card number. */
        DIGITS("[0-9]+", "Ce numéro ne doit contenir que des chiffres"),
        /** Digits, letters and separators — a contract or policy reference. */
        ALPHANUMERIC("[A-Za-z0-9][A-Za-z0-9 /._-]*", "Ce numéro contient un caractère non autorisé");

        private final String pattern;
        private final String message;

        IdentifierKind(String pattern, String message) {
            this.pattern = pattern;
            this.message = message;
        }

        public boolean matches(String value) {
            return value != null && value.matches(pattern);
        }

        public String message() {
            return message;
        }
    }

    private final String code;
    private final String displayName;
    private final String identifierLabel;
    private final IdentifierKind identifierKind;
    private final int minLength;
    private final int maxLength;
    private final boolean fixedAmount;

    Biller(String code, String displayName, String identifierLabel,
           IdentifierKind identifierKind, int minLength, int maxLength, boolean fixedAmount) {
        this.code = code;
        this.displayName = displayName;
        this.identifierLabel = identifierLabel;
        this.identifierKind = identifierKind;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.fixedAmount = fixedAmount;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** What to write above the input box, e.g. "Numéro du compteur". */
    public String getIdentifierLabel() {
        return identifierLabel;
    }

    public IdentifierKind getIdentifierKind() {
        return identifierKind;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /** True when the same amount is due every month, and the bill can therefore be scheduled. */
    public boolean isFixedAmount() {
        return fixedAmount;
    }

    @JsonCreator
    public static Biller fromCode(String value) {
        for (Biller biller : values()) {
            if (biller.code.equalsIgnoreCase(value) || biller.name().equalsIgnoreCase(value)) {
                return biller;
            }
        }
        throw new IllegalArgumentException("Facturier inconnu : " + value);
    }
}
