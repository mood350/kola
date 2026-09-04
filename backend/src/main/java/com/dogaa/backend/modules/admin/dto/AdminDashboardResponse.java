package com.dogaa.backend.modules.admin.dto;

/**
 * Aggregates metrics from the modules Groupe B owns (scoring, scheduling,
 * notification). Wallet/transaction/user/vault volumes (spec DOGAA.md §4.5)
 * belong to Groupe A's modules and are not included until those are merged
 * into main and exposed through a service this module can call.
 */
public record AdminDashboardResponse(
        long totalScheduledTasks,
        long activeScheduledTasks,
        long failedScheduledTasks,
        long totalNotificationsSent,
        long totalNotificationsFailed
) {
}
