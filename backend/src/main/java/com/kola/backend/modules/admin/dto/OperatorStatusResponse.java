package com.kola.backend.modules.admin.dto;

/**
 * Whether a Mobile Money operator's ledger agrees with ours (BACKEND.md 8).
 *
 * @param status {@code reconciled} or {@code discrepancy}
 */
public record OperatorStatusResponse(String name, String status) {
}
