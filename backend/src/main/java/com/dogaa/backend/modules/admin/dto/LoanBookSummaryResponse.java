package com.dogaa.backend.modules.admin.dto;

/**
 * The lending panel of the dashboard (BACKEND.md 5).
 *
 * @param allocatedPct share of the money held by the platform that is currently lent out, 0-100
 */
public record LoanBookSummaryResponse(String outstanding,
                                      int allocatedPct,
                                      String defaultRate,
                                      String defaultRateNote,
                                      String userGrowth,
                                      String activeUsersTotal) {
}
