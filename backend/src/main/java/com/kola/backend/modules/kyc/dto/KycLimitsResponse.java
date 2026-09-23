package com.kola.backend.modules.kyc.dto;

import java.math.BigDecimal;

/** A null ceiling means unlimited, which is what TIER_3 grants. Amounts are XOF. */
public record KycLimitsResponse(BigDecimal perTransaction,
                                BigDecimal daily,
                                BigDecimal monthly,
                                BigDecimal balanceCap,
                                boolean creditEligible) {
}
