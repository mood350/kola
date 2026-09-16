package com.kola.backend.modules.credit.dto;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanResponse(UUID id,
                           Currency currency,
                           BigDecimal principal,
                           BigDecimal collateralAmount,
                           BigDecimal leverageRatio,
                           BigDecimal monthlyRatePercent,
                           BigDecimal interestAmount,
                           BigDecimal penaltyAmount,
                           BigDecimal totalDue,
                           BigDecimal amountRepaid,
                           BigDecimal outstanding,
                           BigDecimal shortfallAmount,
                           LoanStatus status,
                           int scoreAtGrant,
                           Instant disbursedAt,
                           Instant dueAt,
                           Instant settledAt) {
}
