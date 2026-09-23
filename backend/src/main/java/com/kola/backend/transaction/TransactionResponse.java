package com.kola.backend.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String reference,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String receiverCurrency,
        BigDecimal exchangeRate,
        Long walletId,
        String receiverPhoneNumber,
        String receiverCountryCode,
        String description,
        String idempotencyKey,
        /**
         * Page où régler un dépôt encore en attente, s'il en existe une.
         *
         * Nulle sur la quasi-totalité des écritures. Elle n'est rendue qu'au
         * propriétaire de l'opération — les mêmes contrôles que le reste de
         * cette réponse — et cesse d'être utile dès que le webhook a crédité.
         */
        String paymentUrl,
        LocalDateTime createdAt
) {
    public static TransactionResponse fromEntity(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReference(),
                tx.getType(),
                tx.getStatus(),
                tx.getAmount(),
                tx.getFee(),
                tx.getCurrency(),
                tx.getReceiverCurrency(),
                tx.getExchangeRate(),
                tx.getWallet() != null ? tx.getWallet().getId() : null,
                tx.getReceiverPhoneNumber(),
                tx.getReceiverCountryCode(),
                tx.getDescription(),
                tx.getIdempotencyKey(),
                tx.getProviderPaymentUrl(),
                tx.getCreatedAt()
        );
    }
}
