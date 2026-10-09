package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A ledger line. Sender and recipient are Kola accounts when there is one; {@code counterparty}
 * is the outside end (a Mobile Money number, a biller reference) when there is not.
 */
public record ConsoleTransaction(String reference,
                                 TransactionType type,
                                 TransactionStatus status,
                                 Currency currency,
                                 BigDecimal amount,
                                 BigDecimal fee,
                                 UUID senderId,
                                 String senderName,
                                 UUID recipientId,
                                 String recipientName,
                                 String counterparty,
                                 String counterpartyName,
                                 String description,
                                 String failureReason,
                                 Instant createdAt) {
}
