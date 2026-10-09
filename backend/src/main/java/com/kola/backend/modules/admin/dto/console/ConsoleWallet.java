package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.WalletType;

import java.math.BigDecimal;

public record ConsoleWallet(WalletType type, Currency currency, BigDecimal available, BigDecimal locked) {
}
