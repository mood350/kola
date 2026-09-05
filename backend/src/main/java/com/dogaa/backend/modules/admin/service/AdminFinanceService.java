package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.util.BackOfficeFormat;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.modules.admin.dto.LiquidityBucketResponse;
import com.dogaa.backend.modules.admin.dto.OperatorStatusResponse;
import com.dogaa.backend.modules.admin.dto.RevenueResponse;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.transaction.dto.FeeAggregate;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.wallet.dto.WalletAggregate;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the money sits and what it earns (BACKEND.md 8). Read-only throughout — this module
 * computes, it never moves anything.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFinanceService {

    private static final List<LoanStatus> OUTSTANDING =
            List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
    private static final List<LoanStatus> EARNED =
            List.of(LoanStatus.REPAID, LoanStatus.DEFAULTED);

    private final WalletService walletService;
    private final LoanRepository loanRepository;
    private final TransactionService transactionService;

    /**
     * Client money split by what it can be used for.
     *
     * <p>Currencies are pooled here, unlike the per-currency aggregates the dashboard shows: the
     * question this screen answers is how much of the book is mobilisable, and a mixed-currency
     * platform still has one answer to that. It is the reason the tiles quote XOF.
     */
    @Transactional(readOnly = true)
    public List<LiquidityBucketResponse> liquidity() {
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal locked = BigDecimal.ZERO;

        for (WalletAggregate aggregate : walletService.aggregateByCurrency()) {
            available = available.add(nullSafe(aggregate.totalAvailable()));
            locked = locked.add(nullSafe(aggregate.totalLocked()));
        }
        BigDecimal lent = nullSafe(loanRepository.sumOutstanding(OUTSTANDING));

        List<BigDecimal> amounts = List.of(available, locked, lent);
        List<Integer> shares = wholePercentages(amounts);

        return List.of(
                new LiquidityBucketResponse("Disponible clients",
                        BackOfficeFormat.compactAmount(available, Currency.XOF.name()),
                        "Soldes courants mobilisables", shares.get(0)),
                new LiquidityBucketResponse("Épargne bloquée",
                        BackOfficeFormat.compactAmount(locked, Currency.XOF.name()),
                        "Coffres et garanties de prêts", shares.get(1)),
                new LiquidityBucketResponse("Prêté aux clients",
                        BackOfficeFormat.compactAmount(lent, Currency.XOF.name()),
                        "Encours de crédit non remboursé", shares.get(2)));
    }

    /** Commissions collected plus interest earned (DOGAA.md 5.3). */
    @Transactional(readOnly = true)
    public RevenueResponse revenue() {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();

        for (FeeAggregate fee : transactionService.aggregateFeesByType(TransactionStatus.COMPLETED)) {
            labels.add(revenueLabel(fee.type()));
            amounts.add(nullSafe(fee.totalFees()));
        }

        BigDecimal interest = nullSafe(loanRepository.sumInterestEarned(EARNED));
        if (interest.signum() > 0) {
            labels.add("Intérêts sur crédits");
            amounts.add(interest);
        }

        List<Integer> shares = wholePercentages(amounts);
        List<RevenueResponse.RevenueLineResponse> lines = new ArrayList<>(labels.size());
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < labels.size(); i++) {
            lines.add(new RevenueResponse.RevenueLineResponse(
                    labels.get(i),
                    BackOfficeFormat.amount(amounts.get(i), Currency.XOF.name()),
                    shares.get(i)));
            total = total.add(amounts.get(i));
        }

        return new RevenueResponse(lines, BackOfficeFormat.amount(total, Currency.XOF.name()));
    }

    /**
     * Empty until an operator API is connected.
     *
     * <p>Reconciliation means comparing our ledger with the operator's statement, and there is no
     * statement to compare against: {@code ExternalTransferGateway} is still a logging stub. A row
     * saying "reconciled" would be an assertion nothing here can support, so none is returned.
     */
    @Transactional(readOnly = true)
    public List<OperatorStatusResponse> operatorReconciliation() {
        log.debug("Operator reconciliation requested; no operator ledger is connected yet");
        return List.of();
    }

    // --- helpers ----------------------------------------------------------

    private static String revenueLabel(TransactionType type) {
        return switch (type) {
            case P2P_TRANSFER -> "Commissions P2P";
            case MERCHANT_PAYMENT -> "Commissions marchands";
            case CASH_OUT -> "Frais de retrait";
            case BILL_PAYMENT -> "Paiements de factures";
            default -> type.name();
        };
    }

    /**
     * Whole percentages that always add up to 100: the shares are floored and the remainder goes to
     * the largest bucket. Rounding each one on its own gives a bar chart that reads 99 % or 101 %.
     */
    private static List<Integer> wholePercentages(List<BigDecimal> amounts) {
        BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Integer> shares = new ArrayList<>(amounts.size());

        if (total.signum() <= 0) {
            amounts.forEach(amount -> shares.add(0));
            return shares;
        }

        int assigned = 0;
        int largest = 0;
        for (int i = 0; i < amounts.size(); i++) {
            int share = BackOfficeFormat.share(amounts.get(i), total).intValue();
            shares.add(share);
            assigned += share;
            if (amounts.get(i).compareTo(amounts.get(largest)) > 0) {
                largest = i;
            }
        }
        shares.set(largest, shares.get(largest) + (100 - assigned));
        return shares;
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
