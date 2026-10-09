package com.kola.backend.modules.transaction.dto;

import com.kola.backend.common.enums.Currency;

import java.math.BigDecimal;

/** {@code total} is what leaves the wallet: {@code amount} to the beneficiary plus {@code fee}. */
public record FeeQuoteResponse(Currency currency,
                               BigDecimal amount,
                               BigDecimal fee,
                               BigDecimal total) {
}
