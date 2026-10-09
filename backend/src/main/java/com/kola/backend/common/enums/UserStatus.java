package com.kola.backend.common.enums;

public enum UserStatus {
    /** Registered and able to transact. */
    ACTIVE,
    /** Temporarily blocked (too many wrong PIN attempts, admin action). */
    SUSPENDED,
    /** Closed for good; login is refused. */
    CLOSED
}
