package com.kola.backend.modules.transaction.service;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.config.FeeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Turns a transaction into the commission Kola keeps (KOLA.md 4.1 "frais dynamiques selon
 * le niveau KYC", 5.3.A).
 *
 * <p>Two inputs: a base rate per transaction type ({@link FeeProperties}) and a small
 * loyalty rebate for verified users — a higher KYC tier means a lower effective rate. The
 * rebate table is a business input, kept here rather than in config until product settles it.
 */
@Component
@RequiredArgsConstructor
public class FeeCalculator {

    /** Multiplier applied to the base rate. TIER_2+ users, who also gate credit, transact cheaper. */
    private static final Map<KycTier, BigDecimal> TIER_MULTIPLIER = Map.of(
            KycTier.TIER_0, new BigDecimal("1.00"),
            KycTier.TIER_1, new BigDecimal("1.00"),
            KycTier.TIER_2, new BigDecimal("0.80"),
            KycTier.TIER_3, new BigDecimal("0.60"));

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final FeeProperties feeProperties;
    private final FeeScheduleService feeScheduleService;

    /**
     * A grid saved in the back-office wins over the configured base rate: it is already expressed
     * per tier, so the rebate must not be applied a second time on top of it.
     */
    public BigDecimal feeFor(TransactionType type, BigDecimal amount, Currency currency, KycTier tier) {
        if (amount.signum() <= 0) {
            return zero(currency);
        }

        BigDecimal percent = feeScheduleService.percentFor(type, tier)
                .orElseGet(() -> basePercentFor(type)
                        .multiply(TIER_MULTIPLIER.getOrDefault(tier, BigDecimal.ONE)));

        if (percent.signum() == 0) {
            return zero(currency);
        }
        return amount.multiply(percent)
                .divide(HUNDRED, currency.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    /** The rate a tier pays today, in percent — what the back-office grid displays. */
    public BigDecimal effectivePercent(TransactionType type, KycTier tier) {
        return feeScheduleService.percentFor(type, tier)
                .orElseGet(() -> basePercentFor(type)
                        .multiply(TIER_MULTIPLIER.getOrDefault(tier, BigDecimal.ONE)));
    }

    private BigDecimal basePercentFor(TransactionType type) {
        return switch (type) {
            case P2P_TRANSFER -> feeProperties.getP2pPercent();
            case MERCHANT_PAYMENT -> feeProperties.getMerchantPercent();
            case BILL_PAYMENT -> feeProperties.getBillPaymentPercent();
            case CASH_OUT -> feeProperties.getCashOutPercent();
            case CASH_IN -> feeProperties.getCashInPercent();
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal zero(Currency currency) {
        return BigDecimal.ZERO.setScale(currency.getDecimalPlaces(), RoundingMode.UNNECESSARY);
    }
}
