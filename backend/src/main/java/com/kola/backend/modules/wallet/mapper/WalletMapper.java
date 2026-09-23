package com.kola.backend.modules.wallet.mapper;

import com.kola.backend.modules.wallet.dto.WalletResponse;
import com.kola.backend.modules.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getCurrency(),
                wallet.getType(),
                wallet.getAvailableBalance(),
                wallet.getLockedBalance(),
                wallet.getTotalBalance(),
                wallet.getStatus(),
                wallet.getCreatedAt());
    }
}
