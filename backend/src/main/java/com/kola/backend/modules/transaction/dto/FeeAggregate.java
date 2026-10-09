package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.TransactionType;

import java.math.BigDecimal;

/**
 * Commission collected on one kind of movement (KOLA.md 5.3.A) — the raw material of the
 * back-office revenue breakdown.
 */
public record FeeAggregate(TransactionType type, BigDecimal totalFees) {
}
