package com.kola.backend.modules.kyc.service;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.config.KycProperties;
import com.kola.backend.exception.KycLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Enforces the per-tier ceilings.
 *
 * <p>Deliberately free of any wallet or transaction dependency: the caller passes in what has
 * already moved over the period. That keeps this usable from the REST path and from the midnight
 * scheduler alike — a scheduled transfer must hit exactly the same ceiling as a manual one, and it
 * will, because there is only one implementation for both to call.
 */
@Service
@RequiredArgsConstructor
public class KycLimitService {

    private final KycProperties kycProperties;

    public KycProperties.TierLimits limitsFor(KycTier tier) {
        return kycProperties.limitsFor(tier);
    }

    public boolean isCreditEligible(KycTier tier) {
        return kycProperties.limitsFor(tier).isCreditEligible();
    }

    /**
     * @param amount      the outgoing amount, fees excluded
     * @param sentToday   what already left the account in the last 24 hours
     * @param sentThisMonth what already left over the current month
     * @throws KycLimitExceededException when any ceiling of the tier would be crossed
     */
    public void assertCanSend(KycTier tier, BigDecimal amount,
                              BigDecimal sentToday, BigDecimal sentThisMonth) {
        KycProperties.TierLimits limits = kycProperties.limitsFor(tier);

        check(tier, "per-transaction", amount, limits.getPerTransaction());
        check(tier, "daily", sentToday.add(amount), limits.getDaily());
        check(tier, "monthly", sentThisMonth.add(amount), limits.getMonthly());
    }

    /** Checked before crediting a wallet: a low tier may not accumulate an unlimited balance. */
    public void assertCanHold(KycTier tier, BigDecimal resultingBalance) {
        check(tier, "balance", resultingBalance, kycProperties.limitsFor(tier).getBalanceCap());
    }

    private void check(KycTier tier, String ceiling, BigDecimal value, BigDecimal limit) {
        // A null limit means unlimited (TIER_3), which is why this is not a plain compareTo.
        if (limit != null && value.compareTo(limit) > 0) {
            throw new KycLimitExceededException(tier, ceiling, limit);
        }
    }
}
