package com.dogaa.backend.modules.vault.entity;

/** Lifecycle of a {@link Vault}. */
public enum VaultStatus {
    /** Open: can receive deposits and release withdrawals. */
    ACTIVE,
    /** Closed by the owner or force-closed by an admin (DOGAA.md 4.5); funds already released. */
    CLOSED
}
