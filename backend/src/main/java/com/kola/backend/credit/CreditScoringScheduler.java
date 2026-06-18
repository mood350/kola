package com.kola.backend.credit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job planifié qui tourne chaque nuit à 2h00 :
 *  1. Recalcule le score de crédit de tous les utilisateurs qui en ont un
 *  2. Marque les prêts dont la date d'échéance est dépassée comme DEFAULTED
 *
 * On utilise @Scheduled (simple, suffisant pour une instance unique) plutôt
 * que Spring Batch : Spring Batch est plus approprié pour des traitements
 * par lots avec gestion des erreurs/reprise sur des millions de lignes.
 * Ici le volume (utilisateurs Kola) ne justifie pas cette complexité.
 * Si le volume dépasse ~10 000 utilisateurs actifs, migrer vers Batch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreditScoringScheduler {

    private final CreditScoringService creditScoringService;
    private final CreditScoreRepository creditScoreRepository;
    private final LoanService loanService;

    @Scheduled(cron = "0 0 2 * * *") // chaque nuit à 2h00
    public void nightly() {
        rescoreAll();
        detectOverdueLoans();
    }

    private void rescoreAll() {
        List<Long> userIds = creditScoreRepository.findAllUserIdsWithLatestScore();
        log.info("[BATCH] Rescoring nocturne de {} utilisateurs", userIds.size());
        int success = 0, errors = 0;
        for (Long userId : userIds) {
            try {
                creditScoringService.computeAndSave(userId);
                success++;
            } catch (Exception e) {
                log.error("[BATCH] Erreur rescoring user {}", userId, e);
                errors++;
            }
        }
        log.info("[BATCH] Rescoring terminé : {} OK, {} erreurs", success, errors);
    }

    private void detectOverdueLoans() {
        log.info("[BATCH] Détection des prêts en défaut");
        try {
            loanService.markOverdueLoans();
        } catch (Exception e) {
            log.error("[BATCH] Erreur détection impayés", e);
        }
    }
}
