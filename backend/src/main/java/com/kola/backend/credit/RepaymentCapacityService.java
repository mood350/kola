package com.kola.backend.credit;

import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traduit l'usage réel d'un compte en montant empruntable.
 *
 * ═══ LE PRINCIPE ═══
 *
 * Le score répond à « cette personne rembourse-t-elle ses dettes ? ». Ce
 * service répond à « de combien dispose-t-elle chaque mois ? ». Deux personnes
 * également fiables mais aux activités différentes obtiennent donc des montants
 * différents — c'est le comportement recherché, pas un effet de bord.
 *
 * ═══ LE CALCUL, EN QUATRE TEMPS ═══
 *
 * <ol>
 *   <li><b>Les flux</b> sur 90 jours : entrées (dépôts, transferts reçus,
 *       déblocages de coffre) et sorties (retraits, transferts émis, paiements
 *       marchands, remboursements, frais). Trois mois plutôt qu'un seul : un
 *       mois isolé confond une bonne période avec un revenu régulier.</li>
 *   <li><b>Le disponible</b> = entrées − sorties. C'est cette différence, et
 *       elle seule, qui peut servir à rembourser. Un commerçant qui encaisse
 *       500 000 et en dépense 495 000 n'a pas 500 000 de capacité : il en a
 *       5 000.</li>
 *   <li><b>La régularité</b> : un disponible obtenu sur un seul mois actif sur
 *       trois est décoté de moitié. Un revenu ponctuel ne se rembourse pas
 *       comme un revenu récurrent, et cette distinction est invisible dans une
 *       moyenne.</li>
 *   <li><b>Le montant</b> : le prêt se solde en une fois à l'échéance, donc ce
 *       qu'il faut pouvoir accumuler est {@code montant × (1 + taux × durée)}.
 *       On n'engage qu'une part du disponible ({@value #COMMITMENT_RATE}), le
 *       reste devant absorber les imprévus — un emprunteur à qui l'on prend
 *       tout le disponible fait défaut au premier accident.</li>
 * </ol>
 *
 * ═══ CE QUI BORNE ENSUITE ═══
 *
 * Le résultat est enfin plafonné par le palier de score (la confiance) et par
 * la progression (un premier prêt reste petit, quels que soient les flux). Le
 * montant retenu est le plus petit des trois — et l'on retient LEQUEL a
 * contraint, pour pouvoir l'expliquer.
 */
@Service
@RequiredArgsConstructor
public class RepaymentCapacityService {

    /** Fenêtre d'observation. Trois mois : assez pour distinguer régulier d'exceptionnel. */
    private static final int OBSERVATION_DAYS = 90;
    private static final int OBSERVATION_MONTHS = 3;

    /**
     * Part du disponible mensuel qu'un prêt peut engager.
     *
     * 40 % — le reste est le coussin qui absorbe une dépense imprévue. Monter
     * ce taux augmente mécaniquement les montants prêtés ET le taux de défaut :
     * c'est le curseur le plus sensible de tout le module.
     */
    private static final BigDecimal COMMITMENT_RATE = new BigDecimal("0.40");

    /** Montant du tout premier prêt, quel que soit le palier ou les flux. */
    private static final BigDecimal FIRST_LOAN_CEILING = new BigDecimal("50000");

    /** Multiplicateur appliqué au dernier prêt remboursé pour fixer le suivant. */
    private static final BigDecimal GRADUATION_MULTIPLIER = new BigDecimal("2");

    /** Durées proposées, en mois (cf. LoanApplicationRequest : 1 à 12). */
    private static final List<Integer> DURATIONS =
            List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    private static final Set<TransactionType> INFLOWS = Set.of(
            TransactionType.DEPOSIT,
            TransactionType.TRANSFER_IN,
            TransactionType.VAULT_UNLOCK
    );

    private static final Set<TransactionType> OUTFLOWS = Set.of(
            TransactionType.WITHDRAWAL,
            TransactionType.TRANSFER_OUT,
            TransactionType.MERCHANT_PAYMENT,
            TransactionType.SCHEDULED_TRANSFER,
            TransactionType.LOAN_REPAYMENT,
            TransactionType.FEE
    );

    private final TransactionRepository transactionRepository;
    private final LoanRequestRepository loanRequestRepository;

    @Transactional(readOnly = true)
    public LoanCapacity compute(User borrower, CreditTier tier) {
        LocalDateTime since = LocalDateTime.now().minusDays(OBSERVATION_DAYS);

        /* Les entrées se lisent des DEUX côtés du grand livre : un dépôt porte
           l'emprunteur comme `sender`, un transfert reçu le porte comme
           `receiver`. N'interroger qu'un seul côté ferait disparaître la moitié
           des revenus de quelqu'un qui vit de transferts reçus — précisément le
           profil que ce service doit savoir évaluer. */
        List<Transaction> sent = recent(transactionRepository
                .findBySenderIdOrderByCreatedAtDesc(borrower.getId()), since);
        List<Transaction> received = recent(transactionRepository
                .findByReceiverIdOrderByCreatedAtDesc(borrower.getId()), since);

        BigDecimal inflowTotal = sum(sent, INFLOWS)
                .add(sum(received, Set.of(TransactionType.TRANSFER_IN)));
        BigDecimal outflowTotal = sum(sent, OUTFLOWS);

        BigDecimal monthlyInflow = perMonth(inflowTotal);
        BigDecimal monthlyOutflow = perMonth(outflowTotal);
        BigDecimal disposable = monthlyInflow.subtract(monthlyOutflow).max(BigDecimal.ZERO);

        int activeMonths = countActiveMonths(sent, received, since);
        BigDecimal stability = stabilityFactor(activeMonths);
        BigDecimal effectiveDisposable = disposable.multiply(stability);

        BigDecimal graduationCeiling = graduationCeiling(borrower);
        BigDecimal tierCeiling = tier.getMaxLoanAmount();

        Map<Integer, BigDecimal> amounts = new LinkedHashMap<>();
        LoanCapacity.LimitingFactor limiting = activeMonths == 0
                ? LoanCapacity.LimitingFactor.NO_ACTIVITY
                : LoanCapacity.LimitingFactor.CASH_FLOW;

        for (int months : DURATIONS) {
            BigDecimal fromCashFlow = affordableAmount(effectiveDisposable, tier.getMonthlyRate(), months);
            BigDecimal retained = fromCashFlow.min(tierCeiling).min(graduationCeiling);
            amounts.put(months, round(retained));

            /* On retient la contrainte de la durée médiane : c'est celle que
               l'interface affiche par défaut, et celle qui décrit le mieux la
               situation générale de l'emprunteur. */
            if (months == 3 && activeMonths > 0) {
                if (retained.compareTo(fromCashFlow) == 0) {
                    limiting = LoanCapacity.LimitingFactor.CASH_FLOW;
                } else if (retained.compareTo(graduationCeiling) == 0) {
                    limiting = LoanCapacity.LimitingFactor.GRADUATION;
                } else {
                    limiting = LoanCapacity.LimitingFactor.TIER;
                }
            }
        }

        return new LoanCapacity(
                round(monthlyInflow),
                round(monthlyOutflow),
                round(disposable),
                activeMonths,
                stability,
                tierCeiling,
                graduationCeiling,
                amounts,
                limiting
        );
    }

    /* ------------------------------------------------------------------ */

    /**
     * Montant qu'un disponible mensuel permet de rembourser sur une durée.
     *
     * On part de ce qui devra être accumulé — {@code total = P × (1 + r × n)} —
     * et on l'inverse : {@code P = (disponible × n × engagement) / (1 + r × n)}.
     * Calculer sur le principal seul oublierait les intérêts, qui sont pourtant
     * dus au même moment.
     */
    private BigDecimal affordableAmount(BigDecimal monthlyDisposable, BigDecimal monthlyRate, int months) {
        if (monthlyDisposable.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal accumulable = monthlyDisposable
                .multiply(BigDecimal.valueOf(months))
                .multiply(COMMITMENT_RATE);

        BigDecimal repaymentFactor = BigDecimal.ONE
                .add(monthlyRate.multiply(BigDecimal.valueOf(months)));

        return accumulable.divide(repaymentFactor, 2, RoundingMode.DOWN);
    }

    /**
     * Plafond de progression.
     *
     * Le premier prêt est petit quels que soient les flux et le score : c'est
     * ainsi que la microfinance gère le risque quand elle n'a encore rien
     * observé de l'emprunteur. Chaque remboursement à l'heure double ensuite le
     * plafond. L'emprunteur construit son accès au crédit, et Kola construit
     * l'historique de défauts qui lui manque aujourd'hui pour aller plus loin.
     */
    private BigDecimal graduationCeiling(User borrower) {
        List<LoanRequest> loans = loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId());

        BigDecimal largestRepaid = loans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.REPAID)
                .filter(loan -> loan.getDefaultedAt() == null)
                .map(LoanRequest::getRequestedAmount)
                .max(BigDecimal::compareTo)
                .orElse(null);

        if (largestRepaid == null) {
            return FIRST_LOAN_CEILING;
        }
        return largestRepaid.multiply(GRADUATION_MULTIPLIER).max(FIRST_LOAN_CEILING);
    }

    /**
     * Décote pour revenus irréguliers.
     *
     * Trois mois actifs sur trois : aucune décote. Un seul : moitié. Une moyenne
     * ne distingue pas 90 000 XOF reçus une fois de 30 000 reçus trois fois, et
     * ces deux situations ne se remboursent pas de la même façon.
     */
    private BigDecimal stabilityFactor(int activeMonths) {
        return switch (activeMonths) {
            case 0 -> BigDecimal.ZERO;
            case 1 -> new BigDecimal("0.50");
            case 2 -> new BigDecimal("0.75");
            default -> BigDecimal.ONE;
        };
    }

    private int countActiveMonths(List<Transaction> sent, List<Transaction> received, LocalDateTime since) {
        Set<YearMonth> months = java.util.stream.Stream.concat(sent.stream(), received.stream())
                .filter(t -> INFLOWS.contains(t.getType()))
                .map(t -> YearMonth.from(t.getCreatedAt()))
                .collect(Collectors.toSet());
        return months.size();
    }

    private List<Transaction> recent(List<Transaction> transactions, LocalDateTime since) {
        return transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .toList();
    }

    private BigDecimal sum(List<Transaction> transactions, Set<TransactionType> types) {
        return transactions.stream()
                .filter(t -> types.contains(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal perMonth(BigDecimal total) {
        return total.divide(BigDecimal.valueOf(OBSERVATION_MONTHS), 2, RoundingMode.HALF_UP);
    }

    /** Arrondi au franc inférieur : le XOF n'a pas de subdivision. */
    private BigDecimal round(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.DOWN);
    }
}
