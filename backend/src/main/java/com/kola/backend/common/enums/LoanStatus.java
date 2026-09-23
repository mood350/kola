package com.kola.backend.common.enums;

public enum LoanStatus {
    /** Disbursed, collateral frozen, not yet due. */
    ACTIVE,
    /** Past its due date, still within the recovery window. */
    OVERDUE,
    /** Principal, interest and any penalty paid in full; collateral released. */
    REPAID,
    /** Recovery failed: the collateral was seized and any shortfall is still owed. */
    DEFAULTED;

    public boolean isOutstanding() {
        return this == ACTIVE || this == OVERDUE;
    }
}
