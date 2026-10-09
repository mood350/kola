package com.kola.backend.modules.wallet.dto;

import com.kola.backend.common.enums.Currency;
import jakarta.validation.constraints.NotNull;

/** Opens a new wallet for the authenticated user in the given currency. */
public record CreateWalletRequest(@NotNull Currency currency) {
}
