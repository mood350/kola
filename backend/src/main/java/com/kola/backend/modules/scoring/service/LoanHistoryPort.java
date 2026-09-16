package com.kola.backend.modules.scoring.service;

import java.util.UUID;

/**
 * Repayment history, owned by the credit module.
 *
 * <p>A port rather than a direct call, because the dependency runs the other way: credit reads the
 * score to decide what to lend, so scoring cannot import credit without closing a cycle. The credit
 * module supplies the adapter.
 */
public interface LoanHistoryPort {

    LoanHistory historyFor(UUID userId);

    /**
     * @param repaidOnTime loans paid back in full by their due date
     * @param paidLate     loans paid back, but after the grace period
     * @param defaulted    loans recovered from the collateral
     */
    record LoanHistory(int repaidOnTime, int paidLate, int defaulted) {

        public static final LoanHistory NONE = new LoanHistory(0, 0, 0);

        public int total() {
            return repaidOnTime + paidLate + defaulted;
        }
    }
}
