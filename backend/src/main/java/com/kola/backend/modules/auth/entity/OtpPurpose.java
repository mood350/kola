package com.kola.backend.modules.auth.entity;

/** What an OTP unlocks. A code issued for one purpose can never be spent on another. */
public enum OtpPurpose {
    /** Proving ownership of a phone number before an account is created. */
    REGISTRATION,
    /** Recovering access when the PIN is forgotten. */
    PIN_RESET,
    /** Raising the KYC tier from 0 to 1 by proving an email address. */
    EMAIL_VERIFICATION
}
