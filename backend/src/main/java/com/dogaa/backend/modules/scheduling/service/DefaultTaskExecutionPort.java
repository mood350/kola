package com.dogaa.backend.modules.scheduling.service;

import com.dogaa.backend.modules.scheduling.entity.ScheduledTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Placeholder implementation: logs and reports failure. Replace by wiring
 * dogaa-wallet/vault/transaction once those modules are merged into main
 * (see {@link TaskExecutionPort}).
 */
@Service
public class DefaultTaskExecutionPort implements TaskExecutionPort {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskExecutionPort.class);

    @Override
    public boolean execute(ScheduledTask task) {
        log.warn("TaskExecutionPort not wired yet - skipping execution of scheduled task {} ({})", task.getId(), task.getType());
        return false;
    }
}
