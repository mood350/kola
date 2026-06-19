package com.kola.backend.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTransferJob {

    private final ScheduledTransferService scheduledTransferService;

    // S'exécute tous les jours à 00:00 (minuit) précis
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyScheduledTransfers() {
        log.info("---- DÉMARRAGE DU JOB DES VIREMENTS PROGRAMMÉS ----");
        scheduledTransferService.processScheduledTransfers();
        log.info("---- FIN DU JOB DES VIREMENTS PROGRAMMÉS ----");
    }
}