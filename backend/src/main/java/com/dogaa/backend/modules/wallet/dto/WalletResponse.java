package com.dogaa.backend.modules.wallet.dto;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.modules.wallet.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Public view of a wallet. */
public record WalletResponse(UUID id,
                             Currency currency,
                             /** CURRENT ou SAVINGS : les deux comptes ont la meme devise. */
                             WalletType type,
                             BigDecimal availableBalance,
                             BigDecimal lockedBalance,
                             BigDecimal totalBalance,
                             WalletStatus status,
                             Instant createdAt) {
}
