package com.kola.backend.modules.transaction.service;

import com.kola.backend.common.enums.Currency;

import java.math.BigDecimal;

/**
 * Sends money out of Kola to an external destination — a Mobile Money account
 * (Orange Money, MTN, Wave, ...) for external P2P and cash-out, or a utility
 * biller for bill payments (KOLA.md 4.1, 4.6.1). A development stub
 * implements it now; the real operator API plugs in here without touching
 * the transaction service.
 */
public interface ExternalTransferGateway {

    /**
     * @param destination E.164 phone number, or a biller reference for a bill payment
     * @param amount      amount to deliver (fee already taken by Kola)
     * @param currency    transfer currency
     * @param reference   Kola transaction reference, for reconciliation
     * @throws com.kola.backend.exception.ApiException if the operator rejects the payout
     */
    void payout(String destination, BigDecimal amount, Currency currency, String reference);
}
