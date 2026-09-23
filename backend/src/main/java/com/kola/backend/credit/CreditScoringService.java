package com.kola.backend.credit;

import com.kola.backend.aml.TransactionCompletedEvent;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
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
        return persist(user, compute(user));
    }

    /** Résultat d'un calcul, avant toute décision d'écriture. */
    private record Computation(int total, CreditTier tier, String breakdownJson) {}

    /**
     * Applique les huit règles. Ne touche à rien.
     *
     * Séparé de l'écriture parce que le calcul sert deux usages désormais :
     * produire une nouvelle ligne, ou seulement vérifier que rien n'a changé.
     */
    private Computation compute(User user) {
        List<ScoreBreakdown.RuleScore> details = new ArrayList<>();
        int total = 0;

        for (ScoringRule rule : ScoringRule.values()) {
            ScoreBreakdown.RuleScore ruleScore = evaluate(rule, user);
            details.add(ruleScore);
            total += ruleScore.points();
        }

        /* Plafond de sécurité, devenu théorique : les plafonds des règles
           totalisent exactement 100 et un test le garantit (cf. ScoringRule).
           Conservé comme garde-fou si une règle venait à être ajoutée sans
           rééquilibrage. */
        total = Math.min(total, 100);

        return new Computation(total, CreditTier.fromScore(total), serializeDetails(details));
    }

    /** Écrit un nouveau score et retire le drapeau « courant » au précédent. */
    private CreditScore persist(User user, Computation computed) {
        creditScoreRepository.markAllAsNotLatest(user.getId());

        CreditScore score = CreditScore.builder()
                .user(user)
                .score(computed.total())
                .tier(computed.tier())
                .breakdownJson(computed.breakdownJson())
                .maxLoanAmount(computed.tier().getMaxLoanAmount())
                .monthlyRate(computed.tier().getMonthlyRate())
                .expiresAt(LocalDateTime.now().plusDays(SCORE_VALIDITY_DAYS))
                .latest(true)
                .build();

        creditScoreRepository.save(score);
        log.info("Score calculé pour user {} : {}/100 ({})",
                user.getId(), computed.total(), computed.tier());
        return score;
    }

    /**
     * Le calcul redonne-t-il exactement ce qui est déjà enregistré ?
     *
     * Le détail est comparé autant que la note : deux scores de 48 peuvent
     * reposer sur des règles différentes (un point gagné en épargne, un perdu
     * en régularité). Ne comparer que le total masquerait ce mouvement, et
     * l'historique cesserait de raconter ce qui s'est passé.
     */
    private boolean isUnchanged(CreditScore current, Computation computed) {
        return current.getScore() == computed.total()
                && current.getTier() == computed.tier()
                && java.util.Objects.equals(current.getBreakdownJson(), computed.breakdownJson());
    }

    /**
     * Recalcule le score et le renvoie — appelé à chaque consultation.
     *
     * ═══ POURQUOI RECALCULER À CHAQUE LECTURE ═══
     *
     * Parce qu'un score affiché doit être vrai au moment où on le regarde. Le
     * bouton « Recalculer » a disparu des deux applications : il faisait
     * porter à l'utilisateur une mécanique interne — savoir que sa note était
     * périmée, et penser à la rafraîchir. Personne n'a à connaître l'existence
     * d'un cache.
     *
     * ═══ CE QUI EST ÉCRIT, ET CE QUI NE L'EST PAS ═══
     *
     * Le calcul est refait à chaque appel, mais RIEN N'EST PERSISTÉ tant que le
     * résultat est identique au dernier enregistré. C'est ce qui rend ce choix
     * tenable : sans cette comparaison, ouvrir la page trois fois de suite
     * insérerait trois lignes dans `credit_scores`, et l'historique — celui que
     * `GET /credit/score/history` expose et qu'un litige exige de pouvoir
     * relire — se remplirait de doublons jusqu'à devenir illisible.
     *
     * Une ligne n'est donc écrite que lorsque la note, le palier ou le détail
     * ont réellement bougé. L'historique redevient ce qu'il prétend être : la
     * suite des CHANGEMENTS de score.
     */
    @Transactional
    public ScoreBreakdown getFresh(User user) {
        Computation computed = compute(user);
        CreditScore current = creditScoreRepository
                .findByUserIdAndLatestTrue(user.getId())
                .orElse(null);

        if (current != null && isUnchanged(current, computed)) {
            /* Rien n'a bougé : on ne touche pas à la base. La date d'expiration
               du score conservé n'est pas repoussée non plus — elle date sa
               dernière VALEUR, pas sa dernière consultation. */
            return toBreakdown(current);
        }

        return toBreakdown(persist(user, computed));
    }

    /**
     * Recalcule après une opération d'argent.
     *
     * ═══ APRÈS LE COMMIT, ET HORS DU CHEMIN CRITIQUE ═══
     *
     * Deux garanties, calquées sur la surveillance LAB-FT qui écoute le même
     * événement :
     *
     *  1. `AFTER_COMMIT` : on ne score jamais une transaction qui pourrait
     *     encore être annulée.
     *  2. `REQUIRES_NEW` + `try/catch` : un échec de calcul ne doit pas pouvoir
     *     faire échouer — ni même ralentir — un virement déjà validé. Le score
     *     est une lecture de l'activité, jamais une condition de celle-ci.
     *
     * Sans cet écouteur, le score ne bougerait qu'à la prochaine ouverture de
     * la page. Avec lui, le pavé « score » de l'accueil est déjà juste quand
     * l'utilisateur y revient après un dépôt.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        try {
            User user = userRepository.findById(event.userId()).orElse(null);
            if (user == null) return;
            getFresh(user);
        } catch (Exception e) {
            log.error("Échec du recalcul de score après la transaction {}",
                    event.transactionId(), e);
        }
    }

    /**
     * Retourne le score courant, en le recalculant s'il est périmé ou absent.
     *
     * Conservé pour les appelants qui ont besoin d'une VALEUR ENGAGEANTE plutôt
     * que d'un affichage : `LoanService` fige ce score dans le prêt qu'il
     * accorde. Le recalcul systématique de `getFresh` n'y apporterait rien —
     * l'écouteur ci-dessus l'a déjà rafraîchi à la dernière opération.
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

        /* Palier par semaine active, plafonne a 15 — le maximum declare par la
           regle. La formule precedente (semaines x 5) montait a 20 et offrait
           donc 5 points hors bareme a quiconque deposait quatre semaines de
           suite : de quoi compenser l'echec complet d'une autre regle. */
        int pts = switch (Math.min(weeksWithDeposit.size(), 4)) {
            case 0 -> 0;
            case 1 -> 5;
            case 2 -> 9;
            case 3 -> 12;
            default -> 15;
        };
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
            /* 8/20 et non 0 : ne jamais avoir emprunte n'est pas un mauvais
               signal, c'est une absence de signal. Mais 8/20 plafonne le score
               total a 88 — PREMIUM au mieux. Le palier ELITE, et ses deux
               millions, ne s'ouvre qu'apres des remboursements observes. */
            return ruleScore(ScoringRule.LOAN_REPAYMENT_HISTORY, 8, "Aucun historique de prêt");
        }

        long defaults = loans.stream().filter(l -> l.getDefaultedAt() != null).count();
        if (defaults > 0) {
            return ruleScore(ScoringRule.LOAN_REPAYMENT_HISTORY, 0,
                    defaults + " prêt(s) ayant connu un défaut de paiement");
        }

        long repaid = loans.stream().filter(l -> l.getStatus() == LoanStatus.REPAID).count();
        int pts = repaid >= 3 ? 20 : repaid == 2 ? 16 : repaid == 1 ? 12 : 8;

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
            return ruleScore(ScoringRule.VAULT_DISCIPLINE, 3, "Aucun coffre terminé pour l'instant");
        }
        double ratio = (double) respected / total;
        int pts = ratio >= 0.9 ? 10 : ratio >= 0.6 ? 7 : ratio >= 0.3 ? 3 : 0;
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
        /* Bareme reetalonne sur 10 points : il en donnait 15 pour 10 declares.
           Le volume reste un indice d'activite, pas une mesure de capacite —
           c'est RepaymentCapacityService qui traduit les flux en montant. */
        if (volume.compareTo(new BigDecimal("300000")) >= 0) { pts = 10; expl = "Volume excellent (≥ 300 000 XOF/mois)"; }
        else if (volume.compareTo(new BigDecimal("100000")) >= 0) { pts = 8; expl = "Bon volume (≥ 100 000 XOF/mois)"; }
        else if (volume.compareTo(new BigDecimal("25000")) >= 0) { pts = 5; expl = "Volume modéré (≥ 25 000 XOF/mois)"; }
        else if (volume.compareTo(new BigDecimal("5000")) >= 0) { pts = 2; expl = "Volume faible (≥ 5 000 XOF/mois)"; }
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

        /* Ramene a 5 points : c'est le critere le plus facile a fabriquer
           (enregistrer cinq numeros ne coute rien) et le plus faible en
           prediction. Un signal simulable ne doit pas peser lourd. */
        int pts = distinct >= 5 ? 5 : distinct >= 3 ? 3 : distinct >= 1 ? 2 : 0;
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
