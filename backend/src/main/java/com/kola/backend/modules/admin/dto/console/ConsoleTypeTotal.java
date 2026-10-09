package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.TransactionType;

import java.math.BigDecimal;

/** One line of the accounting breakdown: every transaction of one type in the period. */
public record ConsoleTypeTotal(TransactionType type, long count, BigDecimal amount, BigDecimal fees) {
}
