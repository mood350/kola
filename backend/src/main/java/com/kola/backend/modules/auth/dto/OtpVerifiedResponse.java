package com.kola.backend.modules.auth.dto;

/**
 * Proof that the number was verified. It is single-use and must be presented to finish
 * the registration; the phone number is read from it rather than from the request body.
 */
public record OtpVerifiedResponse(String verificationToken, long expiresInSeconds) {
}
