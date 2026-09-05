package com.dogaa.backend.modules.credit.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.common.enums.UserStatus;
import com.dogaa.backend.config.CreditProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.credit.dto.CreditEligibilityResponse;
import com.dogaa.backend.modules.credit.dto.LoanRequest;
import com.dogaa.backend.modules.credit.dto.LoanResponse;
import com.dogaa.backend.modules.credit.entity.Loan;
import com.dogaa.backend.modules.credit.mapper.LoanMapper;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.service.ScoringService;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lending against the savings account (DOGAA.md 4.3).
 *
 * <p>Three questions are answered separately and never confused. <em>May they borrow</em> is
 * decided by hard gates — KYC tier, account state, no loan running, score above the floor.
 * <em>How much</em> comes from the collateral times the leverage their record has earned.
 * <em>At what price</em> comes from the same rung. Keeping them apart is why a rule can be changed
 * without touching the other two.
 *
 * <p>Eligibility is always evaluated live, never read from the nightly snapshot: a KYC document can
 * be revoked at any time, and a score row written last night would happily say "eligible" about an
 * account that was demoted this morning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private static final List<LoanStatus> OUTSTANDING = List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final LoanHistoryAdapter loanHistory;
    private final CreditPolicy policy;
    private final ScoringService scoringService;
    private final UserService userService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final CreditProperties properties;

    // --- eligibility ------------------------------------------------------

    @Transactional
    public CreditEligibilityResponse checkEligibility(UUID userId, Currency currency) {
        User user = userService.getById(userId);
        CreditScoreResponse score = scoringService.getLatestScore(userId);
        Wallet savings = walletService.getSavingsWallet(userId, currency);

        int loansRepaid = loanHistory.loansRepaid(userId);
        BigDecimal collateral = savings.getTotalBalance();
        List<String> blockers = blockers(user, score, collateral, userId);

        Optional<CreditProperties.Rung> rung = policy.rungFor(loansRepaid, score.scoreValue());
        if (rung.isEmpty() && blockers.isEmpty()) {
            blockers.add("Your score does not yet reach a lending tier");
        }

        boolean eligible = blockers.isEmpty() && rung.isPresent();
        BigDecimal leverage = rung.map(CreditProperties.Rung::getLeverage).orElse(BigDecimal.ZERO);
        BigDecimal rate = rung.map(CreditProperties.Rung::getMonthlyRatePercent).orElse(BigDecimal.ZERO);
        BigDecimal maxAmount = eligible
                ? policy.maxLoanAmount(collateral, rung.orElseThrow())
                : BigDecimal.ZERO;
        BigDecimal repayable = eligible ? maxAmount.add(policy.interestOn(maxAmount, rate)) : BigDecimal.ZERO;

        return new CreditEligibilityResponse(
                eligible,
                score.scoreValue(),
                properties.getMinimumScore(),
                // The live tier, not the one frozen in last night's score row: a KYC document can
                // be revoked at any moment, and the snapshot would still say "eligible".
                user.getKycTier(),
                score.breakdown(),
                currency,
                collateral,
                leverage,
                maxAmount,
                rate,
                repayable,
                properties.getTermDays(),
                loansRepaid,
                List.copyOf(blockers));
    }

    /** Everything standing between this user and a loan, in words they can act on. */
    private List<String> blockers(User user, CreditScoreResponse score,
                                  BigDecimal collateral, UUID userId) {
        List<String> blockers = new ArrayList<>();

        if (!user.getKycTier().isAtLeast(KycTier.TIER_2)) {
            blockers.add("Credit requires KYC level TIER_2: submit an identity document");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            blockers.add("Your account is " + user.getStatus().name().toLowerCase());
        }
        if (collateral.compareTo(properties.getMinimumCollateral()) < 0) {
            blockers.add("Your savings account must hold at least "
                    + properties.getMinimumCollateral().toPlainString()
                    + ": a loan is secured against it");
        }
        if (score.scoreValue() < properties.getMinimumScore()) {
            blockers.add("Your score is " + score.scoreValue() + "/100, "
                    + properties.getMinimumScore() + " is required");
        }
        if (loanRepository.existsByUserIdAndStatusIn(userId, OUTSTANDING)) {
            blockers.add("You already have a loan running: repay it before borrowing again");
        }
        long accountAgeDays = ChronoUnit.DAYS.between(user.getCreatedAt(), Instant.now());
        if (accountAgeDays < properties.getMinimumAccountAgeDays()) {
            blockers.add("Your account must be at least "
                    + properties.getMinimumAccountAgeDays() + " days old");
        }
        return blockers;
    }

    // --- borrowing --------------------------------------------------------

    @Transactional
    public LoanResponse requestLoan(UUID userId, LoanRequest request) {
        CreditEligibilityResponse eligibility = checkEligibility(userId, request.currency());
        if (!eligibility.eligible()) {
            throw new ConflictException("Not eligible for credit: "
                    + String.join("; ", eligibility.blockers()));
        }
        if (request.amount().compareTo(eligibility.maxLoanAmount()) > 0) {
            throw new BadRequestException("The most you can borrow is "
                    + eligibility.maxLoanAmount().toPlainString() + " "
                    + request.currency() + ", secured on your savings");
        }

        Wallet savings = walletService.getSavingsWallet(userId, request.currency());
        Wallet current = walletService.getWallet(userId, request.currency());

        BigDecimal interest = policy.interestOn(request.amount(), eligibility.monthlyRatePercent());
        Instant now = Instant.now();

        Loan loan = loanRepository.save(Loan.builder()
                .userId(userId)
                .currency(request.currency())
                .savingsWalletId(savings.getId())
                .currentWalletId(current.getId())
                .collateralAmount(savings.getTotalBalance())
                .leverageRatio(eligibility.leverageRatio())
                .principal(request.amount())
                .monthlyRatePercent(eligibility.monthlyRatePercent())
                .interestAmount(interest)
                .scoreAtGrant(eligibility.score())
                .disbursedAt(now)
                .dueAt(now.plus(properties.getTermDays(), ChronoUnit.DAYS))
                .status(LoanStatus.ACTIVE)
                .build());

        freezeCollateral(savings);
        walletService.credit(current.getId(), request.amount());
        transactionService.recordVaultMovement(TransactionType.LOAN_DISBURSEMENT, userId,
                current.getId(), request.currency(), request.amount(),
                "Loan " + loan.getId());

        log.info("Loan {} granted to {}: {} {} at {}%/month, {}x on {} collateral",
                loan.getId(), userId, request.amount(), request.currency(),
                eligibility.monthlyRatePercent(), eligibility.leverageRatio(),
                loan.getCollateralAmount());

        return loanMapper.toResponse(loan);
    }

    /**
     * Moves the whole savings balance into the locked side.
     *
     * <p>No new rule is needed to forbid withdrawals: the available balance is zero, so the wallet
     * refuses them by itself. Deposits keep working and land straight in the locked balance, which
     * is exactly the behaviour asked for — you may keep adding, you may not take back.
     */
    private void freezeCollateral(Wallet savings) {
        if (savings.getAvailableBalance().signum() > 0) {
            walletService.lock(savings.getId(), savings.getAvailableBalance());
        }
    }

    // --- repayment --------------------------------------------------------

    @Transactional
    public LoanResponse repay(UUID userId, UUID loanId, BigDecimal requestedAmount) {
        Loan loan = getOwnLoan(userId, loanId);
        if (!loan.getStatus().isOutstanding()) {
            throw new ConflictException("This loan is already " + loan.getStatus().name().toLowerCase());
        }

        applyPenaltyIfLate(loan);

        BigDecimal amount = requestedAmount == null
                ? loan.getOutstanding()
                : requestedAmount.min(loan.getOutstanding());
        if (amount.signum() <= 0) {
            throw new BadRequestException("Nothing left to repay on this loan");
        }

        walletService.debit(loan.getCurrentWalletId(), amount);
        transactionService.recordVaultMovement(TransactionType.LOAN_REPAYMENT, userId,
                loan.getCurrentWalletId(), loan.getCurrency(), amount, "Loan " + loan.getId());

        loan.setAmountRepaid(loan.getAmountRepaid().add(amount));
        if (loan.getOutstanding().signum() == 0) {
            settle(loan);
        }
        return loanMapper.toResponse(loanRepository.save(loan));
    }

    /** Full repayment releases the collateral: the savings account becomes spendable again. */
    private void settle(Loan loan) {
        loan.setStatus(LoanStatus.REPAID);
        loan.setSettledAt(Instant.now());

        Wallet savings = walletService.getById(loan.getSavingsWalletId());
        if (savings.getLockedBalance().signum() > 0) {
            walletService.unlock(savings.getId(), savings.getLockedBalance());
        }
        log.info("Loan {} repaid in full; collateral released", loan.getId());
    }

    private void applyPenaltyIfLate(Loan loan) {
        long daysLate = ChronoUnit.DAYS.between(loan.getDueAt(), Instant.now());
        if (daysLate <= 0) {
            return;
        }
        loan.setStatus(LoanStatus.OVERDUE);
        loan.setPenaltyAmount(policy.penaltyFor(loan.getPrincipal(), daysLate));
    }

    // --- recovery ---------------------------------------------------------

    /**
     * Recovery on a loan left unpaid past the recovery window: take what the current account holds,
     * then seize the collateral. Anything still owing is recorded as a shortfall — it keeps the
     * borrower off the ladder, and it is the number that says what this loan actually cost.
     */
    @Transactional
    public LoanResponse recover(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        if (!loan.getStatus().isOutstanding()) {
            return loanMapper.toResponse(loan);
        }
        applyPenaltyIfLate(loan);

        BigDecimal owed = loan.getOutstanding();

        Wallet current = walletService.getById(loan.getCurrentWalletId());
        BigDecimal fromCurrent = current.getAvailableBalance().min(owed);
        if (fromCurrent.signum() > 0) {
            walletService.debit(current.getId(), fromCurrent);
            loan.setAmountRepaid(loan.getAmountRepaid().add(fromCurrent));
            owed = owed.subtract(fromCurrent);
        }

        Wallet savings = walletService.getById(loan.getSavingsWalletId());
        BigDecimal fromCollateral = savings.getLockedBalance().min(owed);
        if (fromCollateral.signum() > 0) {
            walletService.unlock(savings.getId(), fromCollateral);
            walletService.debit(savings.getId(), fromCollateral);
            loan.setAmountRepaid(loan.getAmountRepaid().add(fromCollateral));
            owed = owed.subtract(fromCollateral);
        }

        if (owed.signum() > 0) {
            loan.setStatus(LoanStatus.DEFAULTED);
            loan.setShortfallAmount(owed);
            log.warn("Loan {} defaulted: {} {} unrecovered after seizing the collateral",
                    loan.getId(), owed, loan.getCurrency());
        } else {
            loan.setStatus(LoanStatus.REPAID);
            loan.setSettledAt(Instant.now());
            // Recovered in full, but late: the borrower keeps whatever collateral was left over.
            Wallet refreshed = walletService.getById(loan.getSavingsWalletId());
            if (refreshed.getLockedBalance().signum() > 0) {
                walletService.unlock(refreshed.getId(), refreshed.getLockedBalance());
            }
        }
        return loanMapper.toResponse(loanRepository.save(loan));
    }

    // --- reads ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LoanResponse> listLoans(UUID userId) {
        return loanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(loanMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LoanResponse> activeLoan(UUID userId) {
        return loanRepository.findFirstByUserIdAndStatusIn(userId, OUTSTANDING)
                .map(loanMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<Loan> loansDueForRecovery() {
        return loanRepository.findByStatusInAndDueAtBefore(OUTSTANDING,
                Instant.now().minus(properties.getDefaultAfterDays(), ChronoUnit.DAYS));
    }

    private Loan getOwnLoan(UUID userId, UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));
        if (!loan.getUserId().equals(userId)) {
            // Same answer as a missing loan: never confirm that someone else's loan exists.
            throw new ResourceNotFoundException("Loan not found: " + loanId);
        }
        return loan;
    }
}
