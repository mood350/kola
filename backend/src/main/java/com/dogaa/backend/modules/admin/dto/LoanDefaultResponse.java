package com.dogaa.backend.modules.admin.dto;

/**
 * A loan in the recovery queue (BACKEND.md 7).
 *
 * @param id           loan id, used by the "Relancer" button
 * @param borrowerName who owes it
 * @param amount       what is still owed, pre-formatted
 * @param daysLate     days past the due date
 */
public record LoanDefaultResponse(String id, String borrowerName, String amount, long daysLate) {
}
