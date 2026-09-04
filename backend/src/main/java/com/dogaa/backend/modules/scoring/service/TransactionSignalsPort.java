package com.dogaa.backend.modules.scoring.service;

/**
 * Behavioral signals over the last 30 days (spec DOGAA.md §3.2), owned by
 * dogaa-wallet / dogaa-transaction / dogaa-vault (Groupe A). Each method
 * returns points already normalized to 0-20. Until Groupe A's modules are
 * merged into main, {@link DefaultTransactionSignalsPort} answers with
 * neutral values so scoring can be built and tested in isolation; swap the
 * bean (or update the default impl) once the real services exist.
 */
public interface TransactionSignalsPort {

    int depositRegularityPoints(Long userId);

    int savingsDisciplinePoints(Long userId);

    int transactionDiversityPoints(Long userId);

    int balanceStabilityPoints(Long userId);

    int scheduledReliabilityPoints(Long userId);
}
