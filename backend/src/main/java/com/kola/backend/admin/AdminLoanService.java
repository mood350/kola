package com.kola.backend.admin;

import com.kola.backend.admin.AdminLoanDtos.AdminLoanOverview;
import com.kola.backend.admin.AdminLoanDtos.AdminLoanSummary;
import com.kola.backend.admin.AdminLoanDtos.LoanStatusBucket;
import com.kola.backend.credit.LoanRequestRepository;
import com.kola.backend.credit.LoanStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Consultation du portefeuille de prêts.
 *
 * SERVICE STRICTEMENT EN LECTURE, ET C'EST UN CHOIX. Le cycle de vie d'un prêt
 * est piloté par des règles métier qui vivent dans `LoanService` : le
 * déboursement crédite un portefeuille, le remboursement le débite et met le
 * score à jour, la mise en défaut est prononcée par un batch quotidien sur la
 * date d'échéance. Rien de tout cela ne se réduit à un changement de statut.
 *
 * Exposer ici un « changer le statut » laisserait un administrateur passer un
 * prêt en REPAID sans qu'un franc ait bougé, ou en DISBURSED sans versement —
 * l'écart entre le registre et la réalité comptable serait immédiat et
 * silencieux. Toute action sur un prêt doit donc passer par `LoanService`.
 */
@Service
@RequiredArgsConstructor
public class AdminLoanService {

    private final LoanRequestRepository loanRequestRepository;

    /**
     * Statuts pour lesquels de l'argent est engagé ou dû.
     *
     * DEFAULTED en fait partie : un prêt en défaut n'est pas un prêt effacé,
     * c'est une créance qu'on n'a pas recouvrée. L'exclure de l'encours
     * embellirait le portefeuille exactement là où il se dégrade.
     */
    private static final Set<LoanStatus> OUTSTANDING =
            EnumSet.of(LoanStatus.APPROVED, LoanStatus.DISBURSED, LoanStatus.DEFAULTED);

    @Transactional(readOnly = true)
    public Page<AdminLoanSummary> list(LoanStatus status, Pageable pageable) {
        Page<com.kola.backend.credit.LoanRequest> page = (status == null)
                ? loanRequestRepository.findAllForAdmin(pageable)
                : loanRequestRepository.findByStatusForAdmin(status, pageable);

        return page.map(AdminLoanSummary::fromEntity);
    }

    @Transactional(readOnly = true)
    public AdminLoanSummary detail(Long loanId) {
        return loanRequestRepository.findByIdForAdmin(loanId)
                .map(AdminLoanSummary::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Prêt introuvable : " + loanId));
    }

    /**
     * Volumétrie du portefeuille.
     *
     * Les statuts absents de la base sont réintroduits à zéro : une console qui
     * n'affiche une colonne « en défaut » que lorsqu'un défaut existe fait
     * disparaître l'information au moment précis où son absence est une bonne
     * nouvelle — et le lecteur ne peut plus distinguer « aucun défaut » de
     * « la donnée n'est pas remontée ».
     */
    @Transactional(readOnly = true)
    public AdminLoanOverview overview() {
        List<Object[]> rows = loanRequestRepository.summarizeByStatus();

        List<LoanStatusBucket> buckets = java.util.Arrays.stream(LoanStatus.values())
                .map(status -> rows.stream()
                        .filter(row -> row[0] == status)
                        .findFirst()
                        .map(row -> new LoanStatusBucket(
                                status,
                                ((Number) row[1]).longValue(),
                                toAmount(row[2]),
                                toAmount(row[3])))
                        .orElse(new LoanStatusBucket(status, 0L, BigDecimal.ZERO, BigDecimal.ZERO)))
                .toList();

        long total = buckets.stream().mapToLong(LoanStatusBucket::count).sum();

        BigDecimal outstandingPrincipal = buckets.stream()
                .filter(bucket -> OUTSTANDING.contains(bucket.status()))
                .map(LoanStatusBucket::principal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstandingRepayment = buckets.stream()
                .filter(bucket -> OUTSTANDING.contains(bucket.status()))
                .map(LoanStatusBucket::totalRepayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminLoanOverview(total, outstandingPrincipal, outstandingRepayment, buckets);
    }

    /**
     * `SUM` sur une colonne `numeric` peut remonter en `BigDecimal` comme en
     * `BigInteger` selon le pilote et le dialecte. On normalise plutôt que de
     * transtyper en aveugle, ce qui casserait par `ClassCastException` sur un
     * chemin d'exécution rarement testé.
     */
    private static BigDecimal toAmount(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return BigDecimal.ZERO;
    }
}
