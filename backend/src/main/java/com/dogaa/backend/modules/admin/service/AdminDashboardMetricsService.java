package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.util.BackOfficeFormat;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.modules.admin.dto.AlertResponse;
import com.dogaa.backend.modules.admin.dto.ChartPointResponse;
import com.dogaa.backend.modules.admin.dto.LoanBookSummaryResponse;
import com.dogaa.backend.modules.admin.dto.MetricResponse;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.kyc.entity.KycDocumentStatus;
import com.dogaa.backend.modules.kyc.repository.KycDocumentRepository;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.transaction.repository.TransactionRepository;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.dto.WalletAggregate;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The dashboard figures (BACKEND.md 5).
 *
 * <p>Everything here is computed from live tables — no placeholder is returned as though it were
 * real. A dashboard that invents numbers is worse than one that shows zeros, because zeros are
 * honest about an empty database while invented figures survive into a decision.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardMetricsService {

    private static final String CURRENCY = Currency.XOF.name();
    private static final List<LoanStatus> OUTSTANDING =
            List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("dd/MM", Locale.FRENCH);

    /** Backlog above which the KYC queue is worth flagging on the dashboard. */
    private static final long KYC_BACKLOG_ALERT_THRESHOLD = 10;

    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final KycDocumentRepository kycDocumentRepository;
    private final ScheduledTaskService scheduledTaskService;

    // --- metrics ----------------------------------------------------------

    /** The five tiles, in the order the front-end lays them out — it does no label matching. */
    @Transactional(readOnly = true)
    public List<MetricResponse> metrics() {
        Instant now = Instant.now();
        BigDecimal volume24h = volumeSince(now.minus(24, ChronoUnit.HOURS));
        BigDecimal volumePrevious24h = volumeBetween(now.minus(48, ChronoUnit.HOURS),
                now.minus(24, ChronoUnit.HOURS));

        long users = userService.totalUsers();
        long newUsers = userService.newUsersSince(now.minus(30, ChronoUnit.DAYS));
        BigDecimal outstanding = loanRepository.sumOutstanding(OUTSTANDING);

        return List.of(
                new MetricResponse("Volume 24 h",
                        BackOfficeFormat.compactAmount(volume24h, CURRENCY),
                        BackOfficeFormat.signedPercent(change(volumePrevious24h, volume24h)),
                        volume24h.compareTo(volumePrevious24h) >= 0),

                new MetricResponse("Solde global",
                        BackOfficeFormat.compactAmount(globalBalance(), CURRENCY),
                        BackOfficeFormat.amount(lockedBalance(), CURRENCY) + " bloqués",
                        true),

                new MetricResponse("Croissance utilisateurs",
                        String.valueOf(users),
                        "+" + newUsers + " sur 30 j",
                        newUsers > 0),

                new MetricResponse("Encours de prêts",
                        BackOfficeFormat.compactAmount(outstanding, CURRENCY),
                        loanRepository.findByStatusIn(OUTSTANDING).size() + " prêts en cours",
                        true),

                new MetricResponse("Taux de défaut",
                        BackOfficeFormat.percent(defaultRate()),
                        loanRepository.countByStatus(LoanStatus.DEFAULTED) + " défauts",
                        // A default rate going up is bad news: never render it as an improvement.
                        defaultRate().signum() == 0));
    }

    // --- chart ------------------------------------------------------------

    /**
     * Daily completed volume over the window, each bar scaled against the tallest.
     *
     * @param period "14d" or "30d"; anything else falls back to 14 days
     */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> transactionVolume(String period) {
        int days = "30d".equalsIgnoreCase(period) ? 30 : 14;
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant since = today.minusDays(days - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<LocalDate, BigDecimal> perDay = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : transactionRepository
                .completedAmountsSince(TransactionStatus.COMPLETED, since)) {
            LocalDate day = LocalDate.ofInstant((Instant) row[0], ZoneOffset.UTC);
            perDay.computeIfPresent(day, (key, total) -> total.add((BigDecimal) row[1]));
        }

        BigDecimal tallest = perDay.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<ChartPointResponse> points = new ArrayList<>(days);
        int index = 0;
        for (Map.Entry<LocalDate, BigDecimal> entry : perDay.entrySet()) {
            int height = tallest.signum() == 0
                    ? 0
                    : BackOfficeFormat.share(entry.getValue(), tallest).intValue();
            points.add(new ChartPointResponse(
                    entry.getKey().format(DAY_LABEL), height, ++index == perDay.size()));
        }
        return points;
    }

    // --- alerts -----------------------------------------------------------

    /**
     * Alerts derived from real state rather than a seeded list. An empty result means nothing is
     * wrong, which is a useful thing for the console to be able to say.
     */
    @Transactional(readOnly = true)
    public List<AlertResponse> alerts() {
        List<AlertResponse> alerts = new ArrayList<>();

        List<?> overdue = loanRepository.findByStatusInAndDueAtBeforeOrderByDueAtAsc(
                OUTSTANDING, Instant.now());
        if (!overdue.isEmpty()) {
            alerts.add(new AlertResponse(
                    overdue.size() + " prêt(s) en retard",
                    "Échéance dépassée, recouvrement à surveiller.",
                    "critical"));
        }

        long defaulted = loanRepository.countByStatus(LoanStatus.DEFAULTED);
        if (defaulted > 0) {
            alerts.add(new AlertResponse(
                    defaulted + " prêt(s) en défaut",
                    "Garantie saisie, reliquat éventuellement impayé.",
                    "critical"));
        }

        long pendingKyc = kycDocumentRepository.countByStatus(KycDocumentStatus.PENDING);
        if (pendingKyc >= KYC_BACKLOG_ALERT_THRESHOLD) {
            alerts.add(new AlertResponse(
                    pendingKyc + " documents KYC en attente",
                    "La file de vérification s'allonge.",
                    "warning"));
        }

        long failedTasks = scheduledTaskService.listAll().stream()
                .filter(task -> task.status() == ScheduledTaskStatus.FAILED)
                .count();
        if (failedTasks > 0) {
            alerts.add(new AlertResponse(
                    failedTasks + " transaction(s) programmée(s) en échec",
                    "Solde insuffisant ou restriction KYC au moment de l'exécution.",
                    "warning"));
        }
        return alerts;
    }

    // --- loan book --------------------------------------------------------

    @Transactional(readOnly = true)
    public LoanBookSummaryResponse loanBookSummary() {
        BigDecimal outstanding = loanRepository.sumOutstanding(OUTSTANDING);
        BigDecimal held = globalBalance();
        long newUsers = userService.newUsersSince(Instant.now().minus(30, ChronoUnit.DAYS));
        long defaulted = loanRepository.countByStatus(LoanStatus.DEFAULTED);

        return new LoanBookSummaryResponse(
                BackOfficeFormat.amount(outstanding, CURRENCY),
                // What share of the money the platform holds is currently out on loan.
                BackOfficeFormat.share(outstanding, held.add(outstanding)).intValue(),
                BackOfficeFormat.percent(defaultRate()),
                defaulted + " prêt(s) en défaut sur " + loanRepository.count() + " accordés",
                "+" + newUsers + " sur 30 j",
                String.valueOf(userService.totalUsers()));
    }

    // --- shared computations ---------------------------------------------

    private BigDecimal volumeSince(Instant since) {
        return transactionRepository.sumAmountSince(TransactionStatus.COMPLETED, since);
    }

    private BigDecimal volumeBetween(Instant from, Instant to) {
        return volumeSince(from).subtract(volumeSince(to)).max(BigDecimal.ZERO);
    }

    /**
     * XOF only. Adding a USD balance to an XOF one would produce a number that means nothing, and
     * the dashboard tile has room for a single figure — so it shows the currency that matters here
     * rather than a meaningless sum.
     */
    private BigDecimal globalBalance() {
        return walletService.aggregateByCurrency().stream()
                .filter(aggregate -> aggregate.currency() == Currency.XOF)
                .map(aggregate -> aggregate.totalAvailable().add(aggregate.totalLocked()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal lockedBalance() {
        return walletService.aggregateByCurrency().stream()
                .filter(aggregate -> aggregate.currency() == Currency.XOF)
                .map(WalletAggregate::totalLocked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Defaulted loans over loans granted. Zero rather than undefined while nothing has been lent. */
    private BigDecimal defaultRate() {
        long granted = loanRepository.count();
        if (granted == 0) {
            return BigDecimal.ZERO;
        }
        return BackOfficeFormat.share(
                BigDecimal.valueOf(loanRepository.countByStatus(LoanStatus.DEFAULTED)),
                BigDecimal.valueOf(granted));
    }

    private static BigDecimal change(BigDecimal before, BigDecimal after) {
        if (before == null || before.signum() == 0) {
            return after != null && after.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return BackOfficeFormat.share(after.subtract(before), before);
    }
}
