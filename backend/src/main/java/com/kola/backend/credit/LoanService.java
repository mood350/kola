package com.kola.backend.credit;

import com.kola.backend.exception.ActiveLoanExistsException;
import com.kola.backend.exception.InsufficientCreditScoreException;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.User;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRequestRepository loanRequestRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final CreditScoringService creditScoringService;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    // Score minimum pour être éligible (correspond à BASIC tier, score ≥ 40)
    private static final int MIN_ELIGIBLE_SCORE = CreditTier.BASIC.getMinScore();

    // ═══════════════════════════════════════════════════════════════
    //  DEMANDE DE PRÊT
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LoanDtos.LoanResponse apply(User borrower, LoanDtos.LoanApplicationRequest req) {

        // 1. Un seul prêt actif à la fois
        if (loanRequestRepository.hasActiveLoan(borrower.getId())) {
            throw new ActiveLoanExistsException();
        }

        // 2. Score de crédit (recalculé si périmé)
        ScoreBreakdown breakdown = creditScoringService.getOrCompute(borrower);
        if (breakdown.totalScore() < MIN_ELIGIBLE_SCORE) {
            throw new InsufficientCreditScoreException(breakdown.totalScore(), MIN_ELIGIBLE_SCORE);
        }

        CreditScore scoreSnapshot = creditScoreRepository
                .findByUserIdAndLatestTrue(borrower.getId())
                .orElseThrow();

        // 3. Montant dans la limite du tier
        if (req.requestedAmount().compareTo(scoreSnapshot.getMaxLoanAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Montant demandé (" + req.requestedAmount() + " XOF) supérieur à votre plafond "
                    + scoreSnapshot.getTier().name() + " (" + scoreSnapshot.getMaxLoanAmount() + " XOF)."
            );
        }

        // 4. Calcul du remboursement total : principal × (1 + taux × durée)
        BigDecimal totalRepayment = req.requestedAmount()
                .multiply(BigDecimal.ONE
                        .add(scoreSnapshot.getMonthlyRate()
                                .multiply(BigDecimal.valueOf(req.durationMonths()))))
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Wallet de destination (vérifie appartenance)
        Wallet wallet = walletService.findOwnedWalletOrThrow(borrower, req.walletId());

        LoanRequest loan = LoanRequest.builder()
                .borrower(borrower)
                .wallet(wallet)
                .creditScoreSnapshot(scoreSnapshot)
                .requestedAmount(req.requestedAmount())
                .durationMonths(req.durationMonths())
                .monthlyRate(scoreSnapshot.getMonthlyRate())
                .totalRepayment(totalRepayment)
                .status(LoanStatus.APPROVED) // Approbation automatique si score OK
                .purpose(req.purpose())
                .dueDate(LocalDate.now().plusMonths(req.durationMonths()))
                .build();

        loan = loanRequestRepository.save(loan);

        // 6. Déboursement immédiat : crédite le wallet
        disburseLoan(loan, borrower, wallet);

        log.info("Prêt #{} accordé à user {} : {} XOF sur {} mois",
                loan.getId(), borrower.getId(), req.requestedAmount(), req.durationMonths());
        return LoanDtos.LoanResponse.fromEntity(loan);
    }

    // ═══════════════════════════════════════════════════════════════
    //  REMBOURSEMENT
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LoanDtos.LoanResponse repay(User borrower, Long loanId) {
        LoanRequest loan = findOwnedLoanOrThrow(borrower, loanId);

        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new IllegalStateException(
                    "Ce prêt n'est pas en cours de remboursement (statut : " + loan.getStatus() + ")."
            );
        }

        // Débite le wallet du remboursement total
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(borrower, loan.getWallet().getId());

        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(loan.getTotalRepayment()) < 0) {
            throw new com.kola.backend.exception.InsufficientFundsException(
                    "Solde insuffisant pour rembourser : " + available + " XOF disponibles, "
                    + loan.getTotalRepayment() + " XOF requis."
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(loan.getTotalRepayment()));

        Transaction repayTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.LOAN_REPAYMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(loan.getTotalRepayment())
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(borrower)
                .description("Remboursement prêt #" + loan.getId())
                .build();
        transactionRepository.save(repayTx);

        loan.setStatus(LoanStatus.REPAID);
        loanRequestRepository.save(loan);

        // Recalcul du score (le remboursement améliore le profil)
        creditScoringService.computeAndSave(borrower.getId());

        log.info("Prêt #{} remboursé par user {}", loanId, borrower.getId());
        return LoanDtos.LoanResponse.fromEntity(loan);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<LoanDtos.LoanResponse> getMyLoans(User borrower) {
        return loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId())
                .stream()
                .map(LoanDtos.LoanResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanDtos.LoanResponse getLoan(User borrower, Long loanId) {
        return LoanDtos.LoanResponse.fromEntity(findOwnedLoanOrThrow(borrower, loanId));
    }

    // ═══════════════════════════════════════════════════════════════
    //  JOB BATCH : détection des impayés (appelé par le scheduler)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void markOverdueLoans() {
        List<LoanRequest> overdue = loanRequestRepository.findOverdueLoans();
        overdue.forEach(loan -> {
            loan.setStatus(LoanStatus.DEFAULTED);
            log.warn("Prêt #{} marqué DEFAULTED (user {})", loan.getId(), loan.getBorrower().getId());
        });
        loanRequestRepository.saveAll(overdue);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════════════════

    private void disburseLoan(LoanRequest loan, User borrower, Wallet wallet) {
        wallet.setBalance(wallet.getBalance().add(loan.getRequestedAmount()));

        Transaction disburseTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.LOAN_DISBURSEMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(loan.getRequestedAmount())
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(borrower)
                .description("Déboursement prêt #" + loan.getId())
                .build();
        transactionRepository.save(disburseTx);

        loan.setStatus(LoanStatus.DISBURSED);
    }

    private LoanRequest findOwnedLoanOrThrow(User borrower, Long loanId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Prêt introuvable"));
        if (!loan.getBorrower().getId().equals(borrower.getId())) {
            throw new AccessDeniedException("Ce prêt ne vous appartient pas");
        }
        return loan;
    }
}
