package com.kola.backend.modules.credit.service;

import com.kola.backend.modules.credit.entity.Loan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Nightly recovery on loans left unpaid past the window (KOLA.md 4.3).
 *
 * <p>Runs at 01:00, after the scheduled transfers at midnight and the rescoring at 00:30, so a
 * repayment that a programmed transfer was going to make has already had its chance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanRecoveryScheduler {

    private final CreditService creditService;

    @Scheduled(cron = "${app.scheduling.loan-recovery-cron:0 0 1 * * *}")
    public void recoverOverdueLoans() {
        List<Loan> due = creditService.loansDueForRecovery();
        if (due.isEmpty()) {
            return;
        }
        log.info("Recovering {} overdue loans", due.size());
        for (Loan loan : due) {
            try {
                creditService.recover(loan.getId());
            } catch (Exception ex) {
                log.error("Recovery failed for loan {}", loan.getId(), ex);
            }
        }
    }
}
