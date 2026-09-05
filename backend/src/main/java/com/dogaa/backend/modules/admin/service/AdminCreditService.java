package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.common.enums.NotificationChannel;
import com.dogaa.backend.config.CreditProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ForbiddenException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.admin.dto.CreditStatsResponse;
import com.dogaa.backend.modules.admin.dto.LoanDefaultResponse;
import com.dogaa.backend.modules.admin.dto.TierConfigResponse;
import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.credit.entity.Loan;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.credit.service.CreditLadderService;
import com.dogaa.backend.modules.notification.dto.NotificationRequest;
import com.dogaa.backend.modules.notification.service.NotificationService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The loan book as the back-office sees it (BACKEND.md 7).
 *
 * <p>Reads the real {@code Loan} table and the live leverage ladder; owns no data of its own. The
 * tier editor writes through {@link CreditLadderService}, so a saved change takes effect on the
 * next eligibility check rather than sitting in a table nobody reads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCreditService {

    private static final String MODULE = "credit";
    private static final List<LoanStatus> OUTSTANDING =
            List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);

    private final LoanRepository loanRepository;
    private final UserService userService;
    private final CreditLadderService ladderService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // --- headline figures -------------------------------------------------

    @Transactional(readOnly = true)
    public CreditStatsResponse stats() {
        BigDecimal outstanding = loanRepository.sumOutstanding(OUTSTANDING);
        long defaulted = loanRepository.countByStatus(LoanStatus.DEFAULTED);
        long late = loanRepository.countByStatus(LoanStatus.OVERDUE);
        long granted = loanRepository.count();

        return new CreditStatsResponse(
                AdminFormat.amount(outstanding, Currency.XOF.name()),
                AdminFormat.percent(AdminFormat.share(
                        BigDecimal.valueOf(defaulted), BigDecimal.valueOf(granted))),
                late);
    }

    // --- lending tiers ----------------------------------------------------

    /**
     * The ladder, entry rung first. {@code CreditPolicy} reads it best-first, so the order is
     * flipped here: the screen numbers tiers upwards, from the one a new borrower reaches.
     */
    @Transactional(readOnly = true)
    public List<TierConfigResponse> tierConfig() {
        List<CreditProperties.Rung> ladder = ladderService.current();
        List<TierConfigResponse> tiers = new ArrayList<>(ladder.size());

        for (int i = ladder.size() - 1, tier = 1; i >= 0; i--, tier++) {
            CreditProperties.Rung rung = ladder.get(i);
            tiers.add(new TierConfigResponse(
                    "TIER " + tier,
                    rung.getMinScore(),
                    AdminFormat.amount(rung.getMaxAmount(), Currency.XOF.name()),
                    AdminFormat.monthlyRate(rung.getMonthlyRatePercent())));
        }
        return tiers;
    }

    /**
     * Replaces the ladder with an edited copy.
     *
     * <p>Only the three columns the screen exposes can move. Leverage and the repayment count that
     * unlocks a rung are carried over from the rung in the same position: they are the anti-gaming
     * half of the model (DOGAA.md 4.3) and must not become editable as a side effect of a screen
     * that never showed them.
     */
    @Transactional
    public List<TierConfigResponse> updateTierConfig(CurrentAdmin admin, List<TierConfigResponse> tiers) {
        if (admin.role() != AdminRole.SUPER_ADMIN) {
            throw new ForbiddenException("Seul un super-admin peut modifier les paliers de prêt");
        }

        List<CreditProperties.Rung> ladder = ladderService.current();

        if (tiers.size() != ladder.size()) {
            throw new BadRequestException("Le barème compte " + ladder.size()
                    + " paliers ; " + tiers.size() + " ont été envoyés");
        }

        List<TierConfigResponse> before = tierConfig();
        List<CreditProperties.Rung> bestFirst = new ArrayList<>(ladder.size());

        // The request arrives entry-rung-first; the ladder is stored best-rung-first.
        for (int i = 0; i < ladder.size(); i++) {
            CreditProperties.Rung existing = ladder.get(i);
            TierConfigResponse edited = tiers.get(ladder.size() - 1 - i);
            bestFirst.add(new CreditProperties.Rung(
                    existing.getMinLoansRepaid(),
                    edited.minScore(),
                    existing.getLeverage(),
                    parseRate(edited.monthlyRate()),
                    parseAmount(edited.maxAmount())));
        }

        ladderService.replace(bestFirst, admin.displayName());

        auditService.record(admin, MODULE, "Modification des paliers de prêt",
                AuditService.diff(describe(before), describe(tierConfig())),
                "CreditTierConfig", null);

        log.info("Admin {} updated the lending ladder", admin.email());
        return tierConfig();
    }

    // --- recovery queue ---------------------------------------------------

    /** Loans past due, oldest first — what the "Prêts en défaut" list shows. */
    @Transactional(readOnly = true)
    public List<LoanDefaultResponse> defaults() {
        Instant now = Instant.now();
        return loanRepository
                .findByStatusInAndDueAtBeforeOrderByDueAtAsc(
                        List.of(LoanStatus.OVERDUE, LoanStatus.DEFAULTED), now)
                .stream()
                .map(loan -> toDefault(loan, now))
                .toList();
    }

    /** Nudges a late borrower. The trail records it: a reminder is a contact with a customer. */
    @Transactional
    public void remind(CurrentAdmin admin, UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Ce prêt est introuvable"));

        if (!loan.getStatus().isOutstanding() && loan.getStatus() != LoanStatus.DEFAULTED) {
            throw new BadRequestException("Ce prêt est déjà soldé");
        }

        User borrower = userService.getById(loan.getUserId());
        String amount = AdminFormat.amount(loan.getOutstanding(), currencyOf(loan));

        notificationService.send(new NotificationRequest(
                loan.getUserId(),
                NotificationChannel.SMS,
                "Remboursement en attente",
                "Votre prêt Dogaa de " + amount + " est arrivé à échéance. "
                        + "Approvisionnez votre portefeuille pour éviter des pénalités."));

        auditService.record(admin, MODULE,
                "Relance de " + borrower.getFullName() + " sur un prêt en retard",
                null, "Loan", loanId.toString());

        log.info("Admin {} reminded user {} about loan {}", admin.email(), loan.getUserId(), loanId);
    }

    // --- mapping ----------------------------------------------------------

    private LoanDefaultResponse toDefault(Loan loan, Instant now) {
        String borrower = userService.getById(loan.getUserId()).getFullName();
        long daysLate = ChronoUnit.DAYS.between(loan.getDueAt(), now);

        return new LoanDefaultResponse(
                loan.getId().toString(),
                borrower,
                AdminFormat.amount(loan.getOutstanding(), currencyOf(loan)),
                Math.max(daysLate, 0));
    }

    private static String currencyOf(Loan loan) {
        return loan.getCurrency() == null ? Currency.XOF.name() : loan.getCurrency().name();
    }

    private static String describe(List<TierConfigResponse> tiers) {
        return tiers.stream()
                .map(t -> t.name() + " " + t.minScore() + "/" + t.maxAmount() + "/" + t.monthlyRate())
                .reduce((a, b) -> a + ", " + b)
                .orElse("—");
    }

    // --- parsing the edited strings ---------------------------------------

    /** "150 000 XOF", "150000" → 150000. Spaces of any width are grouping, not decimals. */
    private static BigDecimal parseAmount(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new BadRequestException("Montant maximum illisible : « " + value + " »");
        }
        return new BigDecimal(digits);
    }

    /** "7 %/mois", "7,5 %" → 7 / 7.5. */
    private static BigDecimal parseRate(String value) {
        String number = value.replace(',', '.').replaceAll("[^0-9.]", "");
        if (number.isEmpty() || number.equals(".")) {
            throw new BadRequestException("Taux mensuel illisible : « " + value + " »");
        }
        BigDecimal rate = new BigDecimal(number).setScale(2, RoundingMode.HALF_UP);
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Taux mensuel hors bornes : « " + value + " »");
        }
        return rate;
    }
}
