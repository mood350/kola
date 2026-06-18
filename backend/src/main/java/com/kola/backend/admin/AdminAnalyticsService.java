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
                transactionRepository.sumTotalAmount(),
                transactionRepository.sumTotalFees(),
                walletRepository.sumActiveBalances(),
                vaultRepository.sumActiveLockedAmount(),
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
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<AdminMetric> metrics(List<Object[]> rows, boolean hasAmount) {
        return rows.stream()
                .map(row -> new AdminMetric(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue(),
                        hasAmount ? (BigDecimal) row[2] : BigDecimal.ZERO
                ))
                .toList();
    }

    private List<AdminMetric> walletMetrics(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new AdminMetric(
                        String.valueOf(row[0]),
                        ((Number) row[3]).longValue(),
                        ((BigDecimal) row[1]).add((BigDecimal) row[2])
                ))
                .toList();
    }

    private List<AdminMetric> monthlyMetrics(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new AdminMetric(
                        "%04d-%02d".formatted(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).longValue(),
                        (BigDecimal) row[3]
                ))
                .toList();
    }
}
