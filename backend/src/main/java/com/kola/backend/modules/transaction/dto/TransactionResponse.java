package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Public view of a transaction trace (KOLA.md 4.6.4 dashboard). */
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
