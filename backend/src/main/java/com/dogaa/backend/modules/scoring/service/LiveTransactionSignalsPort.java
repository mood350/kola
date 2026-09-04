package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.vault.entity.VaultStatus;
import com.dogaa.backend.modules.vault.service.VaultService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Real behavioral signals (spec DOGAA.md 3.2) computed over the last 30 days,
 * now that dogaa-wallet/transaction/vault (Groupe A) are merged into main.
 * Replaces the neutral placeholder that used to answer these questions.
 * {@code scheduledReliabilityPoints} needs no port at all: it is Groupe B's
 * own data, read straight from {@link ScheduledTaskService}.
 */
@Service
public class LiveTransactionSignalsPort implements TransactionSignalsPort {

    private static final int WINDOW_DAYS = 30;
    private static final int MAX_POINTS = 20;

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final VaultService vaultService;
    private final ScheduledTaskService scheduledTaskService;

    public LiveTransactionSignalsPort(TransactionService transactionService,
                                       WalletService walletService,
                                       VaultService vaultService,
                                       ScheduledTaskService scheduledTaskService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
        this.vaultService = vaultService;
        this.scheduledTaskService = scheduledTaskService;
    }

    /** Distinct days the wallet was credited by someone else, spec example: "3 times a month". */
    @Override
    public int depositRegularityPoints(UUID userId) {
        long distinctDaysCredited = recentTransactions(userId).stream()
                .filter(tx -> userId.equals(tx.getRecipientId()))
                .map(tx -> tx.getCreatedAt().truncatedTo(ChronoUnit.DAYS))
                .distinct()
                .count();
        return cap((int) Math.round(distinctDaysCredited * (MAX_POINTS / 3.0)));
    }

    /** Vaults currently holding money is the clearest sign of "discipline d'epargne". */
    @Override
    public int savingsDisciplinePoints(UUID userId) {
        long fundedActiveVaults = vaultService.listVaults(userId).stream()
                .filter(v -> v.getStatus() == VaultStatus.ACTIVE)
                .filter(v -> v.getBalance().signum() > 0)
                .count();
        return cap((int) (fundedActiveVaults * 10));
    }

    @Override
    public int transactionDiversityPoints(UUID userId) {
        Set<TransactionType> distinctTypes = recentTransactions(userId).stream()
                .map(Transaction::getType)
                .collect(Collectors.toSet());
        return cap(distinctTypes.size() * 5);
    }

    /** Heuristic: still holding a positive balance beats being swept to zero every cycle. */
    @Override
    public int balanceStabilityPoints(UUID userId) {
        BigDecimal totalAvailable = walletService.listWallets(userId).stream()
                .map(Wallet::getAvailableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalAvailable.signum() > 0 ? MAX_POINTS : 0;
    }

    @Override
    public int scheduledReliabilityPoints(UUID userId) {
        List<ScheduledTaskResponse> tasks = scheduledTaskService.listByUser(userId);
        long completed = tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.COMPLETED).count();
        long failed = tasks.stream().filter(t -> t.status() == ScheduledTaskStatus.FAILED).count();
        long total = completed + failed;
        return total == 0 ? 0 : cap((int) Math.round((completed * (double) MAX_POINTS) / total));
    }

    private List<Transaction> recentTransactions(UUID userId) {
        Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
        return transactionService.history(userId, PageRequest.of(0, 500)).stream()
                .filter(tx -> tx.getCreatedAt().isAfter(since))
                .toList();
    }

    private static int cap(int points) {
        return Math.max(0, Math.min(points, MAX_POINTS));
    }
}
