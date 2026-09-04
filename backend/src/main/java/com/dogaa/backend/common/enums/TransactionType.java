package com.dogaa.backend.common.enums;

/**
 * Every kind of money movement that leaves a transaction trace (DOGAA.md 4.1, 4.2, 4.3).
 * The scheduler (DOGAA.md 4.6) reads this to know how to execute a due item.
 */
public enum TransactionType {
    /** Cash-in from an external Mobile Money account. Free (DOGAA.md 5.3.A). */
    CASH_IN,
    /** Cash-out to an external Mobile Money account. Charged ~1%. */
    CASH_OUT,
    /** Peer-to-peer transfer between two Dogaa users, or to an external number. Charged ~1.5%. */
    P2P_TRANSFER,
    /** Payment to a partner merchant. Charged a fixed or variable commission. */
    MERCHANT_PAYMENT,
    /** Move from a wallet's available balance into a vault (locked balance). Free. */
    VAULT_DEPOSIT,
    /** Release from a vault back to the available balance. Free. */
    VAULT_WITHDRAWAL,
    /** Loan principal paid onto the borrower's wallet. */
    LOAN_DISBURSEMENT,
    /** Principal + interest pulled back at maturity. */
    LOAN_REPAYMENT,
    /** Admin-driven reversal of a disputed transaction (DOGAA.md 4.5). */
    CHARGEBACK;

    public boolean isOutgoing() {
        return this == CASH_OUT || this == P2P_TRANSFER || this == MERCHANT_PAYMENT
                || this == LOAN_REPAYMENT;
    }

    public boolean chargesFee() {
        return this == CASH_OUT || this == P2P_TRANSFER || this == MERCHANT_PAYMENT;
    }
}
