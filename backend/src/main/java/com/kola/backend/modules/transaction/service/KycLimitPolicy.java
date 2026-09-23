package com.kola.backend.modules.transaction.service;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.kyc.service.KycLimitService;
import com.kola.backend.modules.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Gathers what a user has already moved over each period and hands it to {@link KycLimitService},
 * which owns the ceilings (KOLA.md 4.4).
 *
 * <p>This class used to carry a hard-coded table of daily caps and check nothing else. The result
 * was two sets of numbers: the ones in {@code app.kyc.limits.*} that the KYC screen displayed, and
 * the ones in the table that were actually enforced — and they had already drifted apart, TIER_1
 * being told 300 000 while 500 000 went through. The per-transaction, monthly and balance ceilings
 * were configured, displayed, and enforced nowhere at all.
 *
 * <p>So the split is now: this class knows how to <em>measure</em> a period from the transaction
 * history, {@code KycLimitService} knows what the <em>ceilings</em> are. Nothing here decides a
 * limit, which is what stops the two from diverging again.
 *
 * <p>Simplification kept from before: totals are compared against same-currency spend only. A
 * cross-currency normalisation belongs here once an FX rate source exists.
 */
@Component
@RequiredArgsConstructor
public class KycLimitPolicy {

    /**
     * What counts against the ceilings. Savings movements are deliberately absent: moving money
     * between one's own two accounts is not spending, and must not consume a send allowance.
     */
    private static final List<TransactionType> OUTGOING =
            List.of(TransactionType.P2P_TRANSFER, TransactionType.MERCHANT_PAYMENT,
                    TransactionType.CASH_OUT, TransactionType.BILL_PAYMENT);

    private final TransactionRepository transactionRepository;
    private final KycLimitService kycLimitService;

    /**
     * Checks the per-transaction, daily and monthly ceilings in one call.
     *
     * <p>Called from the synchronous API path and from the midnight scheduler, so a scheduled
     * transfer meets exactly the same ceiling as a manual one — "les contrôles KYC vivent à la
     * frontière d'exécution" (KOLA.md 4.6).
     */
    @Transactional(readOnly = true)
    public void checkSendLimits(UUID userId, KycTier tier, Currency currency, BigDecimal amount) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        kycLimitService.assertCanSend(tier, amount,
                sentSince(userId, currency, startOfDay),
                sentSince(userId, currency, startOfMonth));
    }

    /**
     * Checks that crediting a wallet would not take the account past the balance ceiling of its
     * tier.
     *
     * <p>Applied to money the user brings in themselves (cash-in), where the answer — raise your
     * KYC level — is theirs to act on. It is deliberately <b>not</b> applied to an incoming
     * transfer: bouncing a payment because the recipient is near their ceiling punishes the sender
     * for someone else's paperwork, and that is a product call rather than a technical one.
     */
    @Transactional(readOnly = true)
    public void checkResultingBalance(KycTier tier, BigDecimal currentBalance, BigDecimal incoming) {
        kycLimitService.assertCanHold(tier, currentBalance.add(incoming));
    }

    private BigDecimal sentSince(UUID userId, Currency currency, Instant since) {
        return transactionRepository.sumSentSince(
                userId, currency, TransactionStatus.COMPLETED, OUTGOING, since);
    }
}
