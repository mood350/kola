package com.dogaa.backend.config;

import com.dogaa.backend.common.enums.KycTier;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * KYC ceilings and upload rules (DOGAA.md 4.4).
 *
 * <p>Amounts are in XOF. A {@code null} ceiling means "no limit", which is what TIER_3 is for;
 * it is not the same as zero, and the checks treat the two differently on purpose.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.kyc")
public class KycProperties {

    private Map<KycTier, TierLimits> limits = defaultLimits();

    private Upload upload = new Upload();

    public TierLimits limitsFor(KycTier tier) {
        TierLimits tierLimits = limits.get(tier);
        if (tierLimits == null) {
            throw new IllegalStateException("No KYC limits configured for " + tier);
        }
        return tierLimits;
    }

    @Getter
    @Setter
    public static class TierLimits {

        /** Largest single outgoing operation. */
        private BigDecimal perTransaction;

        /** Rolling ceiling over 24 hours. */
        private BigDecimal daily;

        private BigDecimal monthly;

        /** How much the wallets of one user may hold in total. */
        private BigDecimal balanceCap;

        /** Whether this tier may be granted a loan at all (DOGAA.md 4.3: TIER_2 is the gate). */
        private boolean creditEligible;

        static TierLimits of(String perTransaction, String daily, String monthly,
                             String balanceCap, boolean creditEligible) {
            TierLimits tierLimits = new TierLimits();
            tierLimits.perTransaction = amount(perTransaction);
            tierLimits.daily = amount(daily);
            tierLimits.monthly = amount(monthly);
            tierLimits.balanceCap = amount(balanceCap);
            tierLimits.creditEligible = creditEligible;
            return tierLimits;
        }

        private static BigDecimal amount(String value) {
            return value == null ? null : new BigDecimal(value);
        }
    }

    @Getter
    @Setter
    public static class Upload {

        private DataSize maxFileSize = DataSize.ofMegabytes(5);

        /** Deliberately narrow: anything executable or scriptable has no business here. */
        private List<String> allowedContentTypes =
                List.of("image/jpeg", "image/png", "image/webp", "application/pdf");

        /** Where {@code LocalDocumentStorage} writes. Replace the storage bean in production. */
        private String storageDirectory = "var/kyc-documents";
    }

    /**
     * TIER_0 mirrors the 50 000 XOF daily ceiling named in the spec; the rest scale from there.
     * Every value is overridable through {@code app.kyc.limits.<tier>.*}.
     */
    private static Map<KycTier, TierLimits> defaultLimits() {
        Map<KycTier, TierLimits> defaults = new EnumMap<>(KycTier.class);
        defaults.put(KycTier.TIER_0, TierLimits.of("25000", "50000", "200000", "100000", false));
        defaults.put(KycTier.TIER_1, TierLimits.of("100000", "300000", "1500000", "1000000", false));
        defaults.put(KycTier.TIER_2, TierLimits.of("1000000", "3000000", "10000000", "5000000", true));
        defaults.put(KycTier.TIER_3, TierLimits.of(null, null, null, null, true));
        return defaults;
    }
}
