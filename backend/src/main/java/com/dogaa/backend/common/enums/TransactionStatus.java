package com.dogaa.backend.common.enums;

/**
 * Lifecycle of a transaction trace. Synchronous API calls jump straight to
 * {@link #COMPLETED} or raise an error; the scheduler (DOGAA.md 4.6) is what actually
 * produces {@link #PENDING} and {@link #FAILED} rows.
 */
public enum TransactionStatus {
    /** Created but not yet executed (queued by the scheduler). */
    PENDING,
    /** Money moved successfully; the trace is final. */
    COMPLETED,
    /** Execution was refused (insufficient funds, KYC limit, frozen wallet). */
    FAILED,
    /** A previously completed transaction was undone by an admin chargeback (DOGAA.md 4.5). */
    REVERSED
}
