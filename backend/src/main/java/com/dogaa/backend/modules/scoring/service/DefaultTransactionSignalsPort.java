package com.dogaa.backend.modules.scoring.service;

import org.springframework.stereotype.Service;

/**
 * Placeholder implementation: returns neutral (zero) signals. Replace by
 * wiring dogaa-wallet/transaction/vault once those modules are merged into
 * main (see {@link TransactionSignalsPort}).
 */
@Service
public class DefaultTransactionSignalsPort implements TransactionSignalsPort {

    @Override
    public int depositRegularityPoints(Long userId) {
        return 0;
    }

    @Override
    public int savingsDisciplinePoints(Long userId) {
        return 0;
    }

    @Override
    public int transactionDiversityPoints(Long userId) {
        return 0;
    }

    @Override
    public int balanceStabilityPoints(Long userId) {
        return 0;
    }

    @Override
    public int scheduledReliabilityPoints(Long userId) {
        return 0;
    }
}
