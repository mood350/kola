package com.dogaa.backend.modules.vault.mapper;

import com.dogaa.backend.modules.vault.dto.VaultResponse;
import com.dogaa.backend.modules.vault.entity.Vault;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class VaultMapper {

    public VaultResponse toResponse(Vault vault) {
        return new VaultResponse(
                vault.getId(),
                vault.getName(),
                vault.getCurrency(),
                vault.getBalance(),
                vault.getTargetAmount(),
                vault.getTargetDate(),
                progressPercent(vault),
                vault.isGoalReached(),
                vault.getStatus(),
                vault.getDescription(),
                vault.getCreatedAt());
    }

    /** 0-100, capped; null when the vault has no target to measure against. */
    private Integer progressPercent(Vault vault) {
        BigDecimal target = vault.getTargetAmount();
        if (target == null || target.signum() <= 0) {
            return null;
        }
        int pct = vault.getBalance()
                .multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.FLOOR)
                .intValue();
        return Math.min(pct, 100);
    }
}
