package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.config.ScoringProperties;
import com.dogaa.backend.modules.scoring.entity.DailyBalanceSnapshot;
import com.dogaa.backend.modules.scoring.repository.DailyBalanceSnapshotRepository;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * The nightly pass (DOGAA.md 4.3): photograph every balance, then rescore everyone.
 *
 * <p>It runs at 00:30 rather than midnight on purpose. The scheduled-transaction runner fires at
 * 00:00, and reading balances while transfers are being executed would give a score that depends on
 * which job won the race. Half an hour of clearance costs nothing and makes the result reproducible.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringScheduler {

    private final UserService userService;
    private final WalletService walletService;
    private final ScoringService scoringService;
    private final DailyBalanceSnapshotRepository snapshotRepository;
    private final ScoringProperties properties;

    @Scheduled(cron = "${app.scheduling.credit-scoring-cron:0 30 0 * * *}")
    public void runNightlyScoring() {
        List<UUID> userIds = userService.findActiveUserIds();
        log.info("Nightly scoring starting for {} active users", userIds.size());

        int scored = 0;
        int failed = 0;
        for (UUID userId : userIds) {
            try {
                snapshotBalances(userId);
                scoringService.calculateScore(userId);
                scored++;
            } catch (Exception ex) {
                // One user's bad data must not stop the other thousands from being scored.
                failed++;
                log.error("Scoring failed for user {}", userId, ex);
            }
        }
        purgeOldSnapshots();
        log.info("Nightly scoring done: {} scored, {} failed", scored, failed);
    }

    /** One row per user per day. Re-running the job the same day overwrites rather than duplicates. */
    @Transactional
    public void snapshotBalances(UUID userId) {
        List<Wallet> wallets = walletService.listWallets(userId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal savings = BigDecimal.ZERO;
        for (Wallet wallet : wallets) {
            total = total.add(wallet.getTotalBalance());
            available = available.add(wallet.getAvailableBalance());
            if (wallet.getType() == WalletType.SAVINGS) {
                savings = savings.add(wallet.getTotalBalance());
            }
        }

        DailyBalanceSnapshot snapshot = snapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .orElseGet(() -> DailyBalanceSnapshot.builder()
                        .userId(userId)
                        .snapshotDate(today)
                        .build());
        snapshot.setTotalBalance(total);
        snapshot.setAvailableBalance(available);
        snapshot.setSavingsBalance(savings);
        snapshotRepository.save(snapshot);
    }

    /** Snapshots older than twice the window can no longer influence any score. */
    @Transactional
    public void purgeOldSnapshots() {
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(2L * properties.getWindowDays());
        int removed = snapshotRepository.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("Purged {} balance snapshots older than {}", removed, cutoff);
        }
    }
}
