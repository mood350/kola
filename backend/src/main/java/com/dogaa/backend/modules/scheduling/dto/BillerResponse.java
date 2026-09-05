package com.dogaa.backend.modules.scheduling.dto;

import com.dogaa.backend.common.enums.Biller;

/**
 * A biller as the payment form needs it.
 *
 * <p>{@code identifierLabel} exists because the question differs per service: Canal+ asks for the
 * card number printed under the decoder, Cash Power for the meter number, CEET for a customer
 * reference. Sending the label with the biller keeps that wording out of the mobile app, where it
 * would need a release to fix.
 *
 * @param identifierKind   {@code DIGITS} or {@code ALPHANUMERIC} — drives the keyboard to open
 * @param minLength        shortest identifier accepted, {@code maxLength} the longest
 * @param fixedAmount      true when the same sum is due every month, and only then schedulable
 */
public record BillerResponse(String code,
                             String displayName,
                             String identifierLabel,
                             String identifierKind,
                             int minLength,
                             int maxLength,
                             boolean fixedAmount) {

    public static BillerResponse of(Biller biller) {
        return new BillerResponse(biller.getCode(), biller.getDisplayName(),
                biller.getIdentifierLabel(), biller.getIdentifierKind().name(),
                biller.getMinLength(), biller.getMaxLength(), biller.isFixedAmount());
    }
}
