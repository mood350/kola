package com.kola.backend.modules.vault.dto;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.modules.vault.entity.VaultStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Public view of a vault, with goal progress pre-computed for the client. */
public record VaultResponse(UUID id,
                            String name,
                            Currency currency,
                            BigDecimal balance,
                            BigDecimal targetAmount,
                            LocalDate targetDate,
                            Integer progressPercent,
                            boolean goalReached,
                            VaultStatus status,
                            String description,
                            Instant createdAt) {
}
