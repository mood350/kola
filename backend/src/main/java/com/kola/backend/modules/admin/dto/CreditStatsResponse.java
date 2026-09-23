package com.kola.backend.modules.admin.dto;

/**
 * Headline figures of the loan book (BACKEND.md 7).
 *
 * @param outstandingTotal what the book still owes, pre-formatted ("1 860 000 XOF")
 * @param defaultRate      share of granted loans that defaulted ("2,8 %")
 * @param lateLoans        loans past their due date right now
 */
public record CreditStatsResponse(String outstandingTotal, String defaultRate, long lateLoans) {
}
