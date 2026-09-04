package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything needed to write a {@code FAILED} trace when the scheduler (DOGAA.md 4.6.3 step 4)
 * cannot execute a due item. Only {@code type}, {@code currency}, {@code amount} and
 * {@code failureReason} are required; the rest is filled in where the scheduler knows it
 * (e.g. {@code sourceWalletId} is null when the sender has no wallet in that currency).
 */
public record FailedTransactionCommand(
        TransactionType type,
        Currency currency,
        BigDecimal amount,
        BigDecimal fee,
        UUID senderId,
        UUID sourceWalletId,
        UUID recipientId,
        UUID destinationWalletId,
        String counterparty,
        String description,
        String failureReason) {

    /** Minimal form: an outgoing movement from one user that never touched a wallet. */
    public static FailedTransactionCommand of(TransactionType type, Currency currency, BigDecimal amount,
                                              UUID senderId, String counterparty,
                                              String description, String failureReason) {
        return new FailedTransactionCommand(type, currency, amount, null,
                senderId, null, null, null, counterparty, description, failureReason);
    }
}
