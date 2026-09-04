package com.dogaa.backend.modules.transaction.mapper;

import com.dogaa.backend.modules.transaction.dto.TransactionResponse;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getReference(),
                tx.getType(),
                tx.getStatus(),
                tx.getCurrency(),
                tx.getAmount(),
                tx.getFee(),
                tx.getTotalDebited(),
                tx.getCounterparty(),
                tx.getDescription(),
                tx.getFailureReason(),
                tx.getCompletedAt(),
                tx.getCreatedAt());
    }
}
