package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.enums.Currency;

import java.math.BigDecimal;

/**
 * Sends money out of Dogaa to an external Mobile Money account (Orange Money, MTN, Wave, ...)
 * for external P2P and cash-out (DOGAA.md 4.1). A development stub implements it now; the real
 * operator API plugs in here without touching the transaction service.
 */
public interface ExternalTransferGateway {

    /**
     * @param phone     E.164 destination number
     * @param amount    amount to deliver (fee already taken by Dogaa)
     * @param currency  transfer currency
     * @param reference Dogaa transaction reference, for reconciliation
     * @throws com.dogaa.backend.exception.ApiException if the operator rejects the payout
     */
    void payout(String phone, BigDecimal amount, Currency currency, String reference);
}
