package com.kola.backend.vault;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VaultResponse(
        Long id,
        String name,
        String purpose,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        String currency,
        LocalDate unlockDate,
        VaultStatus status,
        Long walletId
) {
    public static VaultResponse fromEntity(Vault vault) {
        return new VaultResponse(
                vault.getId(),
                vault.getName(),
                vault.getPurpose(),
                vault.getTargetAmount(),
                vault.getCurrentAmount(),
                vault.getCurrency(),
                vault.getUnlockDate(),
                vault.getStatus(),
                vault.getWallet().getId()
        );
    }
}
