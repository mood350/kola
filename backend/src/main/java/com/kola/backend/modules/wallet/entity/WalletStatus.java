package com.kola.backend.modules.wallet.entity;

/** Lifecycle of a {@link Wallet}. Admins flip these during dispute/fraud handling (KOLA.md 4.5). */
public enum WalletStatus {
    /** Normal: can send and receive. */
    ACTIVE,
    /** Frozen by an admin: incoming credits still land, every debit is refused. */
    FROZEN,
    /** Closed for good: no movement at all. */
    CLOSED
}
