package com.dogaa.backend.modules.wallet.dto;

import com.dogaa.backend.common.enums.Currency;
import jakarta.validation.constraints.NotNull;

/** Opens a new wallet for the authenticated user in the given currency. */
public record CreateWalletRequest(@NotNull Currency currency) {
}
