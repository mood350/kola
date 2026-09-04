package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;

/**
 * Performs the actual money movement (P2P transfer, merchant payment, vault
 * deposit, bill payment) for a due task. Owned by dogaa-wallet / dogaa-vault /
 * dogaa-transaction (Groupe A). {@link DefaultTaskExecutionPort} always
 * reports failure so due tasks retry/fail visibly instead of silently
 * "succeeding" while those modules are not merged into main yet.
 */
public interface TaskExecutionPort {

    boolean execute(ScheduledTask task);
}
