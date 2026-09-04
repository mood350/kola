package com.dogaa.backend.modules.scoring.service;

import java.util.UUID;

/**
 * Behavioral signals over the last 30 days (spec DOGAA.md §3.2), owned by
 * dogaa-wallet / dogaa-transaction / dogaa-vault (Groupe A). Each method
 * returns points already normalized to 0-20. Until Groupe A's modules are
 * merged into main, {@link DefaultTransactionSignalsPort} answers with
 * neutral values so scoring can be built and tested in isolation; swap the
 * bean (or update the default impl) once the real services exist.
 */
public interface TransactionSignalsPort {

    int depositRegularityPoints(UUID userId);

    int savingsDisciplinePoints(UUID userId);

    int transactionDiversityPoints(UUID userId);

    int balanceStabilityPoints(UUID userId);

    int scheduledReliabilityPoints(UUID userId);
}
