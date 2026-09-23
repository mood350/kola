package com.kola.backend.common.enums;

/**
 * Programmable movement types (spec KOLA.md §4.6.1). Shared with
 * kola-wallet/vault, which trigger these via kola-scheduling.
 */
public enum ScheduledTaskType {
    P2P_TRANSFER,
    MERCHANT_PAYMENT,
    VAULT_DEPOSIT,
    SAVINGS_DEPOSIT,
    BILL_PAYMENT
}
