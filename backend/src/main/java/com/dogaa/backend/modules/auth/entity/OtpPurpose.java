package com.dogaa.backend.modules.auth.entity;

/** What an OTP unlocks. A code issued for one purpose can never be spent on another. */
public enum OtpPurpose {
    /** Proving ownership of a phone number before an account is created. */
    REGISTRATION,
    /** Recovering access when the PIN is forgotten. */
    PIN_RESET
}
