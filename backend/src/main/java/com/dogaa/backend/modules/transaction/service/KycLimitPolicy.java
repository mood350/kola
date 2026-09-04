package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.LimitExceededException;
import com.dogaa.backend.modules.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces the per-tier daily send ceiling from DOGAA.md 4.4. Called on the synchronous API
 * path and by the scheduler (DOGAA.md 4.6) — "les contrôles KYC vivent à la frontière
 * d'exécution".
 *
 * <p>Simplification for now: the ceiling is compared against same-currency spend only. A
 * cross-currency normalisation belongs here once an FX rate source exists.
 */
@Component
@RequiredArgsConstructor
public class KycLimitPolicy {

    /** Daily outgoing cap per tier, in the transaction's own currency units. {@code null} = uncapped. */
    private static final Map<KycTier, BigDecimal> DAILY_SEND_CAP = new EnumMap<>(Map.of(
            KycTier.TIER_0, new BigDecimal("50000"),
            KycTier.TIER_1, new BigDecimal("500000"),
            KycTier.TIER_2, new BigDecimal("5000000")));
    // TIER_3 intentionally absent -> unlimited.

    private static final List<TransactionType> OUTGOING =
            List.of(TransactionType.P2P_TRANSFER, TransactionType.MERCHANT_PAYMENT,
                    TransactionType.CASH_OUT, TransactionType.BILL_PAYMENT);

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public void checkDailySendLimit(UUID userId, KycTier tier, Currency currency, BigDecimal amount) {
        BigDecimal cap = DAILY_SEND_CAP.get(tier);
        if (cap == null) {
            return;
        }
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal alreadySent = transactionRepository.sumSentSince(
                userId, currency, TransactionStatus.COMPLETED, OUTGOING, startOfDay);
        if (alreadySent.add(amount).compareTo(cap) > 0) {
            throw new LimitExceededException(
                    "Daily %s send limit for %s is %s; %s already sent today"
                            .formatted(currency, tier, cap.toPlainString(), alreadySent.toPlainString()));
        }
    }
}
