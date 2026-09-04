package com.dogaa.backend.modules.scoring.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Placeholder implementation: returns neutral (zero) signals. Replace by
 * wiring dogaa-wallet/transaction/vault once those modules are merged into
 * main (see {@link TransactionSignalsPort}).
 */
@Service
public class DefaultTransactionSignalsPort implements TransactionSignalsPort {

    @Override
    public int depositRegularityPoints(UUID userId) {
        return 0;
    }

    @Override
    public int savingsDisciplinePoints(UUID userId) {
        return 0;
    }

    @Override
    public int transactionDiversityPoints(UUID userId) {
        return 0;
    }

    @Override
    public int balanceStabilityPoints(UUID userId) {
        return 0;
    }

    @Override
    public int scheduledReliabilityPoints(UUID userId) {
        return 0;
    }
}
