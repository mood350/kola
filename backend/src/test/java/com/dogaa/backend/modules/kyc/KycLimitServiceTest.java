package com.dogaa.backend.modules.kyc;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.config.KycProperties;
import com.dogaa.backend.exception.KycLimitExceededException;
import com.dogaa.backend.modules.kyc.service.KycLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KycLimitServiceTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private KycLimitService limitService;

    @BeforeEach
    void setUp() {
        limitService = new KycLimitService(new KycProperties());
    }

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    @Test
    void tier0AllowsAnOrdinaryTransferWithinTheDailyCeiling() {
        assertThatCode(() -> limitService.assertCanSend(KycTier.TIER_0, xof("20000"), ZERO, ZERO))
                .doesNotThrowAnyException();
    }

    @Test
    void tier0RefusesASingleTransferAboveThePerTransactionCeiling() {
        assertThatThrownBy(() -> limitService.assertCanSend(KycTier.TIER_0, xof("30000"), ZERO, ZERO))
                .isInstanceOf(KycLimitExceededException.class)
                .hasMessageContaining("per-transaction");
    }

    @Test
    void theDailyCeilingCountsWhatAlreadyLeftTheAccount() {
        // 20 000 alone is fine, but not on top of 40 000 already sent today.
        assertThatThrownBy(() -> limitService.assertCanSend(
                KycTier.TIER_0, xof("20000"), xof("40000"), xof("40000")))
                .isInstanceOf(KycLimitExceededException.class)
                .hasMessageContaining("daily");
    }

    @Test
    void spendingExactlyTheCeilingIsAllowed() {
        assertThatCode(() -> limitService.assertCanSend(
                KycTier.TIER_0, xof("10000"), xof("40000"), xof("40000")))
                .doesNotThrowAnyException();
    }

    @Test
    void theMonthlyCeilingBitesEvenWhenTheDayIsClear() {
        assertThatThrownBy(() -> limitService.assertCanSend(
                KycTier.TIER_1, xof("100000"), ZERO, xof("1450000")))
                .isInstanceOf(KycLimitExceededException.class)
                .hasMessageContaining("monthly");
    }

    @Test
    void tier3HasNoCeilingAtAll() {
        assertThatCode(() -> limitService.assertCanSend(
                KycTier.TIER_3, xof("500000000"), xof("500000000"), xof("500000000")))
                .doesNotThrowAnyException();
        assertThatCode(() -> limitService.assertCanHold(KycTier.TIER_3, xof("999999999")))
                .doesNotThrowAnyException();
    }

    @Test
    void theBalanceCapLimitsWhatALowTierMayAccumulate() {
        assertThatThrownBy(() -> limitService.assertCanHold(KycTier.TIER_0, xof("150000")))
                .isInstanceOf(KycLimitExceededException.class)
                .hasMessageContaining("balance");
    }

    @Test
    void creditOpensAtTier2AsTheSpecRequires() {
        assertThat(limitService.isCreditEligible(KycTier.TIER_0)).isFalse();
        assertThat(limitService.isCreditEligible(KycTier.TIER_1)).isFalse();
        assertThat(limitService.isCreditEligible(KycTier.TIER_2)).isTrue();
        assertThat(limitService.isCreditEligible(KycTier.TIER_3)).isTrue();
    }

    @Test
    void theErrorNamesTheTierAndTheCeilingSoTheAppCanOfferAnUpgrade() {
        assertThatThrownBy(() -> limitService.assertCanSend(KycTier.TIER_0, xof("30000"), ZERO, ZERO))
                .isInstanceOfSatisfying(KycLimitExceededException.class, ex -> {
                    assertThat(ex.getTier()).isEqualTo(KycTier.TIER_0);
                    assertThat(ex.getLimit()).isEqualByComparingTo("25000");
                    assertThat(ex.getCode()).isEqualTo("KYC_LIMIT_EXCEEDED");
                });
    }
}
