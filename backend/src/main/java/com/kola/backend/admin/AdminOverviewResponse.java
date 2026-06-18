package com.kola.backend.admin;

import java.math.BigDecimal;
import java.util.List;

public record AdminOverviewResponse(
        long activeUsers,
        long totalUsers,
        long adminUsers,
        long activeWallets,
        long activeVaults,
        BigDecimal totalTransactionVolume,
        BigDecimal totalFees,
        BigDecimal activeWalletBalance,
        BigDecimal lockedVaultAmount,
        double userGrowthRate,
        double transactionVolumeGrowthRate,
        List<AdminMetric> usersByCountry,
        List<AdminMetric> usersByKycLevel,
        List<AdminMetric> walletsByCurrency,
        List<AdminMetric> transactionsByStatus,
        List<AdminMetric> transactionsByType,
        List<AdminMetric> transactionsByCurrency,
        List<AdminMetric> transactionsByCountry,
        List<AdminMetric> monthlyTransactions
) {
}
