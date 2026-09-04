package com.dogaa.backend.modules.credit.dto;

import com.dogaa.backend.common.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param amount how much to borrow; must not exceed the eligibility ceiling. Borrowing less than
 *               the maximum is allowed and costs less in interest.
 */
public record LoanRequest(@NotNull Currency currency,
                          @NotNull @DecimalMin("1.0") BigDecimal amount) {
}
