package com.kola.backend.modules.transaction;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.config.FeeProperties;
import com.kola.backend.modules.transaction.service.FeeCalculator;
import com.kola.backend.modules.transaction.service.FeeScheduleService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeCalculatorTest {

    /**
     * No back-office grid saved, which is what these cases are about: the configured base rate
     * times the tier multiplier. The service reads its in-memory grid, empty until a version is
     * loaded, so it never reaches the repository here and a null one is enough.
     */
    private final FeeCalculator calculator =
            new FeeCalculator(new FeeProperties(), new FeeScheduleService(null));

    @Test
    void p2pTransferChargesOnePointFivePercentForAnUnverifiedUser() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.P2P_TRANSFER, new BigDecimal("100000"), Currency.XOF, KycTier.TIER_0);

        // KOLA.md 5.3.A: 1 500 XOF on a 100 000 XOF transfer.
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
    void billPaymentChargesOnePercent() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.BILL_PAYMENT, new BigDecimal("20000"), Currency.XOF, KycTier.TIER_0);

        assertThat(fee).isEqualByComparingTo("200");
    }

    @Test
    void freeMovementTypesNeverChargeAFee() {
        BigDecimal fee = calculator.feeFor(
                TransactionType.VAULT_DEPOSIT, new BigDecimal("50000"), Currency.USD, KycTier.TIER_1);

        assertThat(fee).isEqualByComparingTo("0");
    }
}
