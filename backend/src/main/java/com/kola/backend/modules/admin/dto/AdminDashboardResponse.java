package com.kola.backend.modules.admin.dto;

import com.kola.backend.modules.transaction.dto.TransactionAggregate;
import com.kola.backend.modules.wallet.dto.WalletAggregate;

import java.util.List;

/**
 * Aggregates the key metrics spec KOLA.md 4.5 asks for in real time: volume
 * de transactions, solde global, croissance des utilisateurs — plus the
 * Groupe B metrics (scoring/scheduling/notification). Everything here is
 * read through the owning module's service (TransactionService, WalletService,
 * UserService), never through their repositories.
 */
public record AdminDashboardResponse(
        long totalUsers,
        long newUsersLast30Days,
        List<TransactionAggregate> completedTransactionVolume,
        List<WalletAggregate> globalBalance,
        long totalScheduledTasks,
        long activeScheduledTasks,
        long failedScheduledTasks,
        long totalNotificationsSent,
        long totalNotificationsFailed
) {
}
