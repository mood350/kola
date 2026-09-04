package com.dogaa.backend.common.enums;

/**
 * Programmable movement types (spec DOGAA.md §4.6.1). Shared with
 * dogaa-wallet/vault, which trigger these via dogaa-scheduling.
 */
public enum ScheduledTaskType {
    P2P_TRANSFER,
    MERCHANT_PAYMENT,
    VAULT_DEPOSIT,
    BILL_PAYMENT
}
