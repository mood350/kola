package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.enums.Currency;

import java.math.BigDecimal;

/**
 * Sends money out of Dogaa to an external destination — a Mobile Money account
 * (Orange Money, MTN, Wave, ...) for external P2P and cash-out, or a utility
 * biller for bill payments (DOGAA.md 4.1, 4.6.1). A development stub
 * implements it now; the real operator API plugs in here without touching
 * the transaction service.
 */
public interface ExternalTransferGateway {

    /**
     * @param destination E.164 phone number, or a biller reference for a bill payment
     * @param amount      amount to deliver (fee already taken by Dogaa)
     * @param currency    transfer currency
     * @param reference   Dogaa transaction reference, for reconciliation
     * @throws com.dogaa.backend.exception.ApiException if the operator rejects the payout
     */
    void payout(String destination, BigDecimal amount, Currency currency, String reference);
}
