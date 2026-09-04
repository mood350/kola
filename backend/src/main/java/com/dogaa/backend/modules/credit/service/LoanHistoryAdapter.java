package com.dogaa.backend.modules.credit.service;

import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.modules.credit.entity.Loan;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.scoring.service.LoanHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Supplies the scoring module with repayment history without scoring having to import credit. */
@Service
@RequiredArgsConstructor
public class LoanHistoryAdapter implements LoanHistoryPort {

    private final LoanRepository loanRepository;

    @Override
    @Transactional(readOnly = true)
    public LoanHistory historyFor(UUID userId) {
        List<Loan> loans = loanRepository.findByUserIdOrderByCreatedAtDesc(userId);

        int onTime = 0;
        int late = 0;
        int defaulted = 0;
        for (Loan loan : loans) {
            switch (loan.getStatus()) {
                case REPAID -> {
                    if (loan.isSettledOnTime()) {
                        onTime++;
                    } else {
                        late++;
                    }
                }
                case DEFAULTED -> defaulted++;
                default -> {
                    // Still running: it says nothing about repayment yet.
                }
            }
        }
        return new LoanHistory(onTime, late, defaulted);
    }

    /** Loans fully paid back, whether on time or late: what the leverage ladder counts. */
    @Transactional(readOnly = true)
    public int loansRepaid(UUID userId) {
        return (int) loanRepository.countByUserIdAndStatus(userId, LoanStatus.REPAID);
    }
}
