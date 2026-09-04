package com.dogaa.backend.modules.credit.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/** @param amount partial repayment; null or absent repays everything outstanding. */
public record RepaymentRequest(@DecimalMin("1.0") BigDecimal amount) {
}
