package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.modules.vault.entity.VaultStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConsoleVault(UUID id,
                           String name,
                           Currency currency,
                           BigDecimal balance,
                           BigDecimal targetAmount,
                           LocalDate targetDate,
                           VaultStatus status) {
}
