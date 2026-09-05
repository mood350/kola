package com.dogaa.backend.modules.transaction.dto;

import com.dogaa.backend.common.enums.TransactionType;

import java.math.BigDecimal;

/**
 * Commission collected on one kind of movement (DOGAA.md 5.3.A) — the raw material of the
 * back-office revenue breakdown.
 */
public record FeeAggregate(TransactionType type, BigDecimal totalFees) {
}
