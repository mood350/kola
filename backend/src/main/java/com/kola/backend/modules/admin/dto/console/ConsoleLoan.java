package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param outstanding principal + interest + penalty − repaid, never below zero
 */
public record ConsoleLoan(UUID id,
                          UUID userId,
                          String userName,
                          Currency currency,
                          BigDecimal principal,
                          BigDecimal interest,
                          BigDecimal penalty,
                          BigDecimal repaid,
                          BigDecimal outstanding,
                          LoanStatus status,
                          Instant disbursedAt,
                          Instant dueAt,
                          Instant settledAt) {
}
