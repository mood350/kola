package com.dogaa.backend.modules.transaction.dto;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Public view of a transaction trace (DOGAA.md 4.6.4 dashboard). */
public record TransactionResponse(UUID id,
                                  String reference,
                                  TransactionType type,
                                  TransactionStatus status,
                                  Currency currency,
                                  BigDecimal amount,
                                  BigDecimal fee,
                                  BigDecimal totalDebited,
                                  String counterparty,
                                  String counterpartyName,
                                  String description,
                                  String failureReason,
                                  Instant completedAt,
                                  Instant createdAt) {
}
