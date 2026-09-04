package com.dogaa.backend.modules.transaction;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.config.FeeProperties;
import com.dogaa.backend.modules.transaction.service.FeeCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeCalculatorTest {

    private final FeeCalculator calculator = new FeeCalculator(new FeeProperties());

    @Test
    void p2pTransferChargesOnePointFivePercentForAnUnverifiedUser() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.P2P_TRANSFER, new BigDecimal("100000"), Currency.XOF, KycTier.TIER_0);

        // DOGAA.md 5.3.A: 1 500 XOF on a 100 000 XOF transfer.
        assertThat(fee).isEqualByComparingTo("1500");
    }

    @Test
    void higherKycTiersTransactCheaper() {
        BigDecimal tier0 = calculator.feeFor(
                TransactionType.P2P_TRANSFER, new BigDecimal("100000"), Currency.XOF, KycTier.TIER_0);
        BigDecimal tier3 = calculator.feeFor(
                TransactionType.P2P_TRANSFER, new BigDecimal("100000"), Currency.XOF, KycTier.TIER_3);

        assertThat(tier3).isLessThan(tier0);
    }

    @Test
    void xofFeeHasNoMinorUnits() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.CASH_OUT, new BigDecimal("12345"), Currency.XOF, KycTier.TIER_0);

        assertThat(fee.scale()).isZero();
    }

    @Test
    void cashInIsFree() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.CASH_IN, new BigDecimal("500000"), Currency.XOF, KycTier.TIER_0);

        assertThat(fee).isEqualByComparingTo("0");
    }

    @Test
    void freeMovementTypesNeverChargeAFee() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.VAULT_DEPOSIT, new BigDecimal("50000"), Currency.USD, KycTier.TIER_1);

        assertThat(fee).isEqualByComparingTo("0");
    }
}
