package com.kola.backend.modules.scheduling.service;

import com.kola.backend.modules.scheduling.entity.ScheduledTask;

/**
 * Performs the actual money movement (P2P transfer, merchant payment, vault
 * deposit, bill payment) for a due task. Owned by kola-wallet / kola-vault /
 * kola-transaction (Groupe A). {@link DefaultTaskExecutionPort} always
 * reports failure so due tasks retry/fail visibly instead of silently
 * "succeeding" while those modules are not merged into main yet.
 */
public interface TaskExecutionPort {

    boolean execute(ScheduledTask task);
}
