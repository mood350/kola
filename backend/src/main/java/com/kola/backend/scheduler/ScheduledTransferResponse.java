package com.kola.backend.scheduler;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScheduledTransferResponse(
        Long id,
        ScheduledTransfer.Frequency frequency,
        int executionDay,
        BigDecimal amount,
        String currency,
        String description,
        ScheduledTransfer.ScheduledStatus status,
        LocalDateTime lastExecutedAt,
        LocalDateTime nextExecutionDate,
        Long walletId,
        Long targetVaultId,
        String targetVaultName
) {
    public static ScheduledTransferResponse fromEntity(ScheduledTransfer st) {
        return new ScheduledTransferResponse(
                st.getId(),
                st.getFrequency(),
                st.getExecutionDay(),
                st.getAmount(),
                st.getCurrency(),
                st.getDescription(),
                st.getStatus(),
                st.getLastExecutedAt(),
                st.getNextExecutionDate(),
                st.getWallet() != null ? st.getWallet().getId() : null,
                st.getTargetVault() != null ? st.getTargetVault().getId() : null,
                st.getTargetVault() != null ? st.getTargetVault().getName() : null
        );
    }
}
