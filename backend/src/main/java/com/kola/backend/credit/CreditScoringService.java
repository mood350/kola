package com.kola.backend.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.vault.Vault;
import com.kola.backend.vault.VaultRepository;
import com.kola.backend.vault.VaultStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditScoringService {

    private final CreditScoreRepository creditScoreRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final TransactionRepository transactionRepository;
    private final VaultRepository vaultRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final int SCORE_VALIDITY_DAYS = 30;

    // ═══════════════════════════════════════════════════════════════
    //  API PUBLIQUE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calcule (ou recalcule) le score d'un utilisateur et le persiste.
     * Appelé à la demande (auto-calcul au premier appel) ou par le job batch.
     */
    @Transactional
    public CreditScore computeAndSave(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        List<ScoreBreakdown.RuleScore> details = new ArrayList<>();
        int total = 0;

        // Calcul de chaque règle
        for (ScoringRule rule : ScoringRule.values()) {
            ScoreBreakdown.RuleScore ruleScore = evaluate(rule, user);
            details.add(ruleScore);
            total += ruleScore.points();
        }

        total = Math.min(total, 100); // plafond de sécurité
        CreditTier tier = CreditTier.fromScore(total);

        // Invalider le score précédent avant de sauvegarder le nouveau
        creditScoreRepository.markAllAsNotLatest(userId);

        CreditScore score = CreditScore.builder()
                .user(user)
                .score(total)
                .tier(tier)
                .breakdownJson(serializeDetails(details))
                .maxLoanAmount(tier.getMaxLoanAmount())
                .monthlyRate(tier.getMonthlyRate())
                .expiresAt(LocalDateTime.now().plusDays(SCORE_VALIDITY_DAYS))
                .latest(true)
                .build();

        creditScoreRepository.save(score);
        log.info("Score calculé pour user {} : {}/100 ({})", userId, total, tier);
        return score;
    }

    /**
     * Retourne le score courant, en le recalculant s'il est périmé ou absent.
     */
    @Transactional
    public ScoreBreakdown getOrCompute(User user) {
        CreditScore current = creditScoreRepository
                .findByUserIdAndLatestTrue(user.getId())
                .orElse(null);

        if (current == null || current.getExpiresAt().isBefore(LocalDateTime.now())) {
            current = computeAndSave(user.getId());
        }

        return toBreakdown(current);
    }

    @Transactional(readOnly = true)
    public List<ScoreBreakdown> getHistory(Long userId) {
        return creditScoreRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toBreakdown)
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    //  RÈGLES DE SCORING
    // ═══════════════════════════════════════════════════════════════

    private ScoreBreakdown.RuleScore evaluate(ScoringRule rule, User user) {
        return switch (rule) {
            case ACCOUNT_SENIORITY -> scoreSeniority(user);
            case KYC_LEVEL         -> scoreKyc(user);
            case DEPOSIT_REGULARITY -> scoreDepositRegularity(user);
            case LOAN_REPAYMENT_HISTORY -> scoreLoanRepaymentHistory(user);
            case VAULT_DISCIPLINE  -> scoreVaultDiscipline(user);
            case TRANSACTION_VOLUME -> scoreTransactionVolume(user);
            case EXPENSE_INCOME_RATIO -> scoreExpenseIncomeRatio(user);
            case BENEFICIARY_DIVERSITY -> scoreBeneficiaryDiversity(user);
        };
    }

    /** Ancienneté : 0 pts < 1 mois, 4 pts < 3 mois, 7 pts < 6 mois, 10 pts ≥ 6 mois */
    private ScoreBreakdown.RuleScore scoreSeniority(User user) {
        long days = ChronoUnit.DAYS.between(
                user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : LocalDate.now(),
                LocalDate.now()
        );
        int pts = days < 30 ? 0 : days < 90 ? 4 : days < 180 ? 7 : 10;
        return ruleScore(ScoringRule.ACCOUNT_SENIORITY, pts,
                pts == 10 ? "Compte actif depuis plus de 6 mois" :
                pts == 7  ? "Compte actif depuis 3 à 6 mois" :
                pts == 4  ? "Compte actif depuis 1 à 3 mois" :
                            "Compte ouvert il y a moins d'un mois");
    }

    /** KYC : TIER_0 = 3, TIER_1 = 7, TIER_2 = 12, TIER_3 = 15 */
    private ScoreBreakdown.RuleScore scoreKyc(User user) {
        int pts = switch (user.getKycLevel()) {
            case TIER_0 -> 3;
            case TIER_1 -> 7;
            case TIER_2 -> 12;
            case TIER_3 -> 15;
        };
        return ruleScore(ScoringRule.KYC_LEVEL, pts,
                "Niveau KYC : " + user.getKycLevel().name());
    }

    /**
     * Régularité des dépôts sur 30 jours.
     * On compte les semaines avec au moins 1 dépôt (4 semaines max = 20 pts).
     */
    private ScoreBreakdown.RuleScore scoreDepositRegularity(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Transaction> deposits = transactionRepository
                .findBySenderIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .toList();

        Set<Integer> weeksWithDeposit = deposits.stream()
                .map(t -> t.getCreatedAt().getDayOfYear() / 7)
                .collect(Collectors.toSet());

        int pts = Math.min(weeksWithDeposit.size(), 4) * 5; // 5 pts par semaine active, max 20
        return ruleScore(ScoringRule.DEPOSIT_REGULARITY, pts,
                weeksWithDeposit.size() + " semaine(s) avec dépôt sur les 4 dernières");
    }

    /**
     * Historique de remboursement.
     *
     * Un défaut, même régularisé depuis, plafonne la note à 0 : c'est le
     * signal de risque le plus fort dont on dispose, et il est lu sur
     * defaultedAt et non sur le statut courant, sinon un remboursement tardif
     * effacerait le défaut de l'historique.
     *
     * L'absence de prêt donne une note neutre (7/15) et non zéro : ne jamais
     * avoir emprunté n'est pas un mauvais signal, c'est une absence de signal.
     * Noter 0 aurait exclu tout nouveau client du premier prêt, rendant la
     * règle auto-réalisatrice.
     */
    private ScoreBreakdown.RuleScore scoreLoanRepaymentHistory(User user) {
        List<LoanRequest> loans = loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(user.getId());

        if (loans.isEmpty()) {
            return ruleScore(ScoringRule.LOAN_REPAYMENT_HISTORY, 7, "Aucun historique de prêt");
        }

        long defaults = loans.stream().filter(l -> l.getDefaultedAt() != null).count();
        if (defaults > 0) {
            return ruleScore(ScoringRule.LOAN_REPAYMENT_HISTORY, 0,
                    defaults + " prêt(s) ayant connu un défaut de paiement");
        }

        long repaid = loans.stream().filter(l -> l.getStatus() == LoanStatus.REPAID).count();
        int pts = repaid >= 3 ? 15 : repaid == 2 ? 12 : repaid == 1 ? 10 : 7;

        return ruleScore(ScoringRule.LOAN_REPAYMENT_HISTORY, pts,
                repaid == 0
                        ? "Prêt en cours, aucun incident à ce jour"
                        : repaid + " prêt(s) remboursé(s) sans incident");
    }

    /**
     * Discipline épargne : ratio coffres respectés jusqu'à l'échéance
     * vs coffres fermés prématurément.
     */
    private ScoreBreakdown.RuleScore scoreVaultDiscipline(User user) {
        List<Vault> vaults = vaultRepository.findByOwnerId(user.getId());
        long total = vaults.stream()
                .filter(v -> v.getStatus() != VaultStatus.ACTIVE)
                .count();
        long respected = vaults.stream()
                .filter(v -> v.getStatus() == VaultStatus.UNLOCKED)
                .count();

        if (total == 0) {
            return ruleScore(ScoringRule.VAULT_DISCIPLINE, 5, "Aucun coffre terminé pour l'instant");
        }
        double ratio = (double) respected / total;
        int pts = ratio >= 0.9 ? 15 : ratio >= 0.6 ? 10 : ratio >= 0.3 ? 5 : 0;
        return ruleScore(ScoringRule.VAULT_DISCIPLINE, pts,
                respected + "/" + total + " coffre(s) respecté(s) jusqu'à l'échéance (" +
                Math.round(ratio * 100) + "%)");
    }

    /**
     * Volume de transactions mensuel (entrées + sorties, hors frais/vault).
     * 5k XOF = 3 pts, 25k = 7 pts, 100k = 12 pts, 300k+ = 15 pts.
     */
    private ScoreBreakdown.RuleScore scoreTransactionVolume(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        BigDecimal volume = transactionRepository
                .findBySenderIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                // TRANSFER_IN exclu au même titre que FEE : cette ligne est la
                // contrepartie du TRANSFER_OUT déjà comptée, et elle porte le
                // même `sender` (l'émetteur). Sans ce filtre, un transfert
                // interne gonflerait le volume de l'émetteur du double de son
                // montant réel — et donc son score de crédit.
                .filter(t -> t.getType() != TransactionType.FEE
                        && t.getType() != TransactionType.TRANSFER_IN)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int pts;
        String expl;
        if (volume.compareTo(new BigDecimal("300000")) >= 0) { pts = 15; expl = "Volume excellent (≥ 300 000 XOF/mois)"; }
        else if (volume.compareTo(new BigDecimal("100000")) >= 0) { pts = 12; expl = "Bon volume (≥ 100 000 XOF/mois)"; }
        else if (volume.compareTo(new BigDecimal("25000")) >= 0) { pts = 7; expl = "Volume modéré (≥ 25 000 XOF/mois)"; }
        else if (volume.compareTo(new BigDecimal("5000")) >= 0) { pts = 3; expl = "Volume faible (≥ 5 000 XOF/mois)"; }
        else { pts = 0; expl = "Volume insuffisant ce mois (< 5 000 XOF)"; }

        return ruleScore(ScoringRule.TRANSACTION_VOLUME, pts, expl);
    }

    /**
     * Ratio dépenses/revenus sur 30 jours.
     * Bon ratio (< 70% dépenses) = score élevé.
     */
    private ScoreBreakdown.RuleScore scoreExpenseIncomeRatio(User user) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Transaction> txs = transactionRepository
                .findBySenderIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(since))
                .toList();

        BigDecimal income = txs.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = txs.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL || t.getType() == TransactionType.TRANSFER_OUT)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return ruleScore(ScoringRule.EXPENSE_INCOME_RATIO, 0, "Aucun revenu détecté ce mois");
        }

        double ratio = expenses.divide(income, 4, java.math.RoundingMode.HALF_UP).doubleValue();
        int pts;
        String expl;
        if (ratio < 0.5)       { pts = 15; expl = "Excellent ratio (" + Math.round(ratio * 100) + "% des revenus dépensés)"; }
        else if (ratio < 0.7)  { pts = 11; expl = "Bon ratio (" + Math.round(ratio * 100) + "% des revenus dépensés)"; }
        else if (ratio < 0.9)  { pts = 6;  expl = "Ratio acceptable (" + Math.round(ratio * 100) + "% des revenus dépensés)"; }
        else                   { pts = 2;  expl = "Ratio tendu (" + Math.round(ratio * 100) + "% des revenus dépensés)"; }

        return ruleScore(ScoringRule.EXPENSE_INCOME_RATIO, pts, expl);
    }

    /** Diversité bénéficiaires : ≥ 5 bénéficiaires distincts = 10 pts */
    private ScoreBreakdown.RuleScore scoreBeneficiaryDiversity(User user) {
        long distinct = transactionRepository
                .findBySenderIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .filter(t -> t.getReceiverPhoneNumber() != null)
                .map(Transaction::getReceiverPhoneNumber)
                .distinct()
                .count();

        int pts = distinct >= 5 ? 10 : distinct >= 3 ? 7 : distinct >= 1 ? 4 : 0;
        return ruleScore(ScoringRule.BENEFICIARY_DIVERSITY, pts,
                distinct + " bénéficiaire(s) distinct(s) contacté(s)");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private ScoreBreakdown.RuleScore ruleScore(ScoringRule rule, int pts, String explanation) {
        return new ScoreBreakdown.RuleScore(rule, rule.getLabel(), pts, rule.getMaxPoints(), explanation);
    }

    private ScoreBreakdown toBreakdown(CreditScore cs) {
        List<ScoreBreakdown.RuleScore> details = deserializeDetails(cs.getBreakdownJson());
        return new ScoreBreakdown(
                cs.getScore(),
                cs.getTier(),
                cs.getMaxLoanAmount(),
                cs.getMonthlyRate(),
                cs.getCreatedAt(),
                cs.getExpiresAt(),
                details
        );
    }

    private String serializeDetails(List<ScoreBreakdown.RuleScore> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.error("Erreur sérialisation breakdown", e);
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<ScoreBreakdown.RuleScore> deserializeDetails(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ScoreBreakdown.RuleScore.class));
        } catch (Exception e) {
            log.error("Erreur désérialisation breakdown", e);
            return List.of();
        }
    }
}
