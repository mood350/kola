package com.kola.backend.admin;

import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.user.UserRepository;
import com.kola.backend.vault.VaultRepository;
import com.kola.backend.vault.VaultStatus;
import com.kola.backend.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final VaultRepository vaultRepository;
    private final TransactionRepository transactionRepository;

    public AdminOverviewResponse overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime currentMonthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonthStart.plusMonths(1);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime sixMonthsAgo = currentMonthStart.minusMonths(5);

        long currentUsers = userRepository.countByCreatedAtBetween(currentMonthStart, nextMonthStart);
        long previousUsers = userRepository.countByCreatedAtBetween(previousMonthStart, currentMonthStart);

        BigDecimal currentVolume = transactionRepository.sumAmountBetween(currentMonthStart, nextMonthStart);
        BigDecimal previousVolume = transactionRepository.sumAmountBetween(previousMonthStart, currentMonthStart);

        return new AdminOverviewResponse(
                userRepository.countByEnabledTrueAndAccountLockedFalse(),
                userRepository.count(),
                userRepository.countByRoleName("ADMIN"),
                walletRepository.countByActiveTrue(),
                vaultRepository.countByStatus(VaultStatus.ACTIVE),
                // Sécurité null au cas où la BDD retourne null
                transactionRepository.sumTotalAmount() != null ? transactionRepository.sumTotalAmount() : BigDecimal.ZERO,
                transactionRepository.sumTotalFees() != null ? transactionRepository.sumTotalFees() : BigDecimal.ZERO,
                walletRepository.sumActiveBalances() != null ? walletRepository.sumActiveBalances() : BigDecimal.ZERO,
                vaultRepository.sumActiveLockedAmount() != null ? vaultRepository.sumActiveLockedAmount() : BigDecimal.ZERO,
                growthRate(BigDecimal.valueOf(currentUsers), BigDecimal.valueOf(previousUsers)),
                growthRate(currentVolume, previousVolume),
                metrics(userRepository.countUsersByCountry(), false),
                metrics(userRepository.countUsersByKycLevel(), false),
                walletMetrics(walletRepository.summarizeByCurrency()),
                metrics(transactionRepository.summarizeByStatus(), true),
                metrics(transactionRepository.summarizeByType(), true),
                metrics(transactionRepository.summarizeByCurrency(), true),
                metrics(transactionRepository.summarizeByCountry(), true),
                monthlyMetrics(transactionRepository.summarizeMonthlySince(sixMonthsAgo))
        );
    }

    private double growthRate(BigDecimal current, BigDecimal previous) {
        // Sécurité : on traite le cas où les montants seraient nulls
        BigDecimal safeCurrent = current != null ? current : BigDecimal.ZERO;
        BigDecimal safePrevious = previous != null ? previous : BigDecimal.ZERO;

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return safeCurrent.subtract(safePrevious)
                .multiply(BigDecimal.valueOf(100))
                .divide(safePrevious, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<AdminMetric> metrics(List<Object[]> rows, boolean hasAmount) {
        if (rows == null || rows.isEmpty()) return List.of();

        return rows.stream()
                .map(row -> new AdminMetric(
                        String.valueOf(row[0]),
                        row[1] != null ? ((Number) row[1]).longValue() : 0L,
                        hasAmount && row.length > 2 && row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO
                ))
                .toList();
    }

    private List<AdminMetric> walletMetrics(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        return rows.stream()
                .map(row -> {
                    BigDecimal balance = row.length > 1 && row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
                    BigDecimal locked = row.length > 2 && row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    long count = row.length > 3 && row[3] != null ? ((Number) row[3]).longValue() : 0L;

                    return new AdminMetric(
                            String.valueOf(row[0]),
                            count,
                            balance.add(locked)
                    );
                })
                .toList();
    }

    private List<AdminMetric> monthlyMetrics(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        return rows.stream()
                .map(row -> new AdminMetric(
                        "%04d-%02d".formatted(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        row[2] != null ? ((Number) row[2]).longValue() : 0L,
                        row.length > 3 && row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO
                ))
                .toList();
    }
}