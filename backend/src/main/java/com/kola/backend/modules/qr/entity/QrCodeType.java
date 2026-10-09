package com.kola.backend.modules.qr.entity;

/** The two things a QR code can be in a wallet. */
public enum QrCodeType {

    /**
     * The user's permanent "carte de visite": it names an account and nothing else. Reusable,
     * never expires, carries no amount — the payer types what they want to send.
     */
    STATIC,

    /**
     * A claim for a precise amount: the payer confirms rather than types. Expires, and is good for
     * exactly one payment — a receipt someone photographs must not be payable twice.
     */
    PAYMENT_REQUEST
}
