package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.config.ScoringProperties;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.scoring.entity.DailyBalanceSnapshot;
import com.dogaa.backend.modules.scoring.repository.DailyBalanceSnapshotRepository;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gathers the raw measurements the {@link ScoreCalculator} works from.
 *
 * <p>Two rules run through everything here. Amounts below the materiality threshold are dropped
 * before anything is counted — a 100 XOF transfer costs nothing to fabricate, so it must earn
 * nothing. And a user's own movements between their own wallets are excluded, because moving money
 * from your left pocket to your right is not an inflow, a payment, or a counterparty.
 */
@Service
@RequiredArgsConstructor
public class ScoringDataCollector {

    private static final int MAX_TRANSACTIONS_READ = 1000;

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final ScheduledTaskService scheduledTaskService;
    private final DailyBalanceSnapshotRepository snapshotRepository;
    private final LoanHistoryPort loanHistoryPort;
    private final ScoringProperties properties;

    @Transactional(readOnly = true)
    public ScoringInputs collect(UUID userId) {
        int windowDays = properties.getWindowDays();
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        Instant previousWindowStart = Instant.now().minus(2L * windowDays, ChronoUnit.DAYS);

        List<Wallet> wallets = walletService.listWallets(userId);
        Set<UUID> ownWalletIds = wallets.stream().map(Wallet::getId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> savingsWalletIds = wallets.stream()
                .filter(w -> w.getType() == WalletType.SAVINGS)
                .map(Wallet::getId)
                .collect(java.util.stream.Collectors.toSet());

        List<Transaction> all = transactionService
                .history(userId, PageRequest.of(0, MAX_TRANSACTIONS_READ))
                .getContent().stream()
                .filter(tx -> tx.getCreatedAt() != null && tx.getCreatedAt().isAfter(previousWindowStart))
                .toList();

        List<Transaction> window = all.stream()
                .filter(tx -> tx.getCreatedAt().isAfter(windowStart))
                .toList();
        List<Transaction> settled = window.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.COMPLETED)
                .filter(this::isMaterial)
                .toList();

        BigDecimal totalInflow = sum(settled.stream()
                .filter(tx -> isIncoming(tx, userId, ownWalletIds))
                .toList());
        BigDecimal totalOutflow = sum(settled.stream()
                .filter(tx -> isOutgoing(tx, userId, ownWalletIds))
                .toList());

        BigDecimal savingsDeposits = sum(settled.stream()
                .filter(tx -> isSavingsInflow(tx, savingsWalletIds))
                .toList());
        BigDecimal savingsWithdrawals = sum(settled.stream()
                .filter(tx -> isSavingsOutflow(tx, savingsWalletIds))
                .toList());

        List<LocalDate> inflowDays = settled.stream()
                .filter(tx -> isIncoming(tx, userId, ownWalletIds))
                .map(tx -> LocalDate.ofInstant(tx.getCreatedAt(), ZoneOffset.UTC))
                .distinct()
                .sorted()
                .toList();

        BalanceStats balance = balanceStats(userId, wallets, windowDays);
        LoanHistoryPort.LoanHistory loans = loanHistoryPort.historyFor(userId);
        List<ScheduledTaskResponse> tasks = scheduledTaskService.listByUser(userId);

        return new ScoringInputs(
                totalInflow,
                totalOutflow,
                savingsDeposits.subtract(savingsWithdrawals).max(BigDecimal.ZERO),
                savingsDeposits,
                savingsWithdrawals,
                (int) tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.COMPLETED).count(),
                (int) tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.FAILED).count(),
                balance.average(),
                balance.standardDeviation(),
                averageDailySpend(totalOutflow, windowDays),
                (int) window.stream()
                        .filter(tx -> tx.getStatus() == TransactionStatus.FAILED)
                        .filter(tx -> userId.equals(tx.getSenderId()))
                        .count(),
                inflowDays.size(),
                intervalCoefficientOfVariation(inflowDays),
                trend(all, window, userId, ownWalletIds, windowStart),
                distinctOutgoingTypes(settled, userId, ownWalletIds),
                distinctCounterparties(settled, userId, ownWalletIds),
                loans.repaidOnTime(),
                loans.paidLate(),
                loans.defaulted());
    }

    // --- classification ---------------------------------------------------

    private boolean isMaterial(Transaction tx) {
        return tx.getAmount() != null
                && tx.getAmount().compareTo(properties.getMaterialityThreshold()) >= 0;
    }

    /** Money entering from outside. A move between the user's own wallets is not an inflow. */
    private boolean isIncoming(Transaction tx, UUID userId, Set<UUID> ownWalletIds) {
        if (isInternalMove(tx, ownWalletIds)) {
            return false;
        }
        return userId.equals(tx.getRecipientId()) || tx.getType() == TransactionType.CASH_IN;
    }

    private boolean isOutgoing(Transaction tx, UUID userId, Set<UUID> ownWalletIds) {
        if (isInternalMove(tx, ownWalletIds)) {
            return false;
        }
        return userId.equals(tx.getSenderId()) && tx.getType().isOutgoing();
    }

    private boolean isInternalMove(Transaction tx, Set<UUID> ownWalletIds) {
        return tx.getSourceWalletId() != null
                && tx.getDestinationWalletId() != null
                && ownWalletIds.contains(tx.getSourceWalletId())
                && ownWalletIds.contains(tx.getDestinationWalletId());
    }

    /** Into the savings account or a vault: both are money the user chose to put away. */
    private boolean isSavingsInflow(Transaction tx, Set<UUID> savingsWalletIds) {
        return tx.getType() == TransactionType.VAULT_DEPOSIT
                || (tx.getDestinationWalletId() != null
                && savingsWalletIds.contains(tx.getDestinationWalletId()));
    }

    private boolean isSavingsOutflow(Transaction tx, Set<UUID> savingsWalletIds) {
        return tx.getType() == TransactionType.VAULT_WITHDRAWAL
                || (tx.getSourceWalletId() != null
                && savingsWalletIds.contains(tx.getSourceWalletId()));
    }

    private int distinctOutgoingTypes(List<Transaction> settled, UUID userId, Set<UUID> ownWalletIds) {
        return (int) settled.stream()
                .filter(tx -> isOutgoing(tx, userId, ownWalletIds))
                .map(Transaction::getType)
                .filter(type -> type == TransactionType.MERCHANT_PAYMENT
                        || type == TransactionType.P2P_TRANSFER
                        || type == TransactionType.BILL_PAYMENT)
                .distinct()
                .count();
    }

    private int distinctCounterparties(List<Transaction> settled, UUID userId, Set<UUID> ownWalletIds) {
        Set<String> counterparties = new HashSet<>();
        for (Transaction tx : settled) {
            if (!isOutgoing(tx, userId, ownWalletIds)) {
                continue;
            }
            if (tx.getRecipientId() != null && !tx.getRecipientId().equals(userId)) {
                counterparties.add(tx.getRecipientId().toString());
            } else if (tx.getCounterparty() != null && !tx.getCounterparty().isBlank()) {
                counterparties.add(tx.getCounterparty());
            }
        }
        return counterparties.size();
    }

    // --- derived statistics ----------------------------------------------

    /**
     * Daily balance over the window. Falls back to what the wallets hold right now when the
     * nightly job has not written enough history yet, which is the state of every account on
     * its first days.
     */
    private BalanceStats balanceStats(UUID userId, List<Wallet> wallets, int windowDays) {
        List<DailyBalanceSnapshot> snapshots = snapshotRepository
                .findByUserIdAndSnapshotDateGreaterThanEqual(
                        userId, LocalDate.now(ZoneOffset.UTC).minusDays(windowDays));

        if (snapshots.size() < 2) {
            BigDecimal current = wallets.stream()
                    .map(Wallet::getTotalBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new BalanceStats(current, BigDecimal.ZERO);
        }

        List<BigDecimal> balances = snapshots.stream().map(DailyBalanceSnapshot::getTotalBalance).toList();
        BigDecimal mean = balances.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(balances.size()), 2, RoundingMode.HALF_UP);

        double variance = balances.stream()
                .mapToDouble(b -> Math.pow(b.subtract(mean).doubleValue(), 2))
                .average()
                .orElse(0);

        return new BalanceStats(mean, BigDecimal.valueOf(Math.sqrt(variance))
                .setScale(2, RoundingMode.HALF_UP));
    }

    /** How evenly inflows are spread: 0 means perfectly regular, 1 or more means bunched up. */
    private double intervalCoefficientOfVariation(List<LocalDate> inflowDays) {
        if (inflowDays.size() < 3) {
            return 1.0;
        }
        List<Long> gaps = new ArrayList<>(inflowDays.size() - 1);
        for (int i = 1; i < inflowDays.size(); i++) {
            gaps.add(ChronoUnit.DAYS.between(inflowDays.get(i - 1), inflowDays.get(i)));
        }
        double mean = gaps.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean <= 0) {
            return 1.0;
        }
        double variance = gaps.stream()
                .mapToDouble(gap -> Math.pow(gap - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance) / mean;
    }

    /** This window's inflow against the previous one of the same length. */
    private ScoringInputs.Trend trend(List<Transaction> all, List<Transaction> window,
                                      UUID userId, Set<UUID> ownWalletIds, Instant windowStart) {
        BigDecimal current = sum(window.stream()
                .filter(tx -> tx.getStatus() == TransactionStatus.COMPLETED)
                .filter(this::isMaterial)
                .filter(tx -> isIncoming(tx, userId, ownWalletIds))
                .toList());
        BigDecimal previous = sum(all.stream()
                .filter(tx -> tx.getCreatedAt().isBefore(windowStart))
                .filter(tx -> tx.getStatus() == TransactionStatus.COMPLETED)
                .filter(this::isMaterial)
                .filter(tx -> isIncoming(tx, userId, ownWalletIds))
                .toList());

        if (previous.signum() == 0) {
            // No comparison point: a new account is neither growing nor shrinking.
            return ScoringInputs.Trend.STABLE;
        }
        BigDecimal change = current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP);
        if (change.compareTo(new BigDecimal("0.10")) >= 0) {
            return ScoringInputs.Trend.GROWING;
        }
        if (change.compareTo(new BigDecimal("-0.10")) <= 0) {
            return ScoringInputs.Trend.DECLINING;
        }
        return ScoringInputs.Trend.STABLE;
    }

    private static BigDecimal averageDailySpend(BigDecimal totalOutflow, int windowDays) {
        return totalOutflow.divide(BigDecimal.valueOf(windowDays), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record BalanceStats(BigDecimal average, BigDecimal standardDeviation) {
    }
}
