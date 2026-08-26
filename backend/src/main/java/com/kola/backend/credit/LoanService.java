package com.kola.backend.credit;

import com.kola.backend.exception.ActiveLoanExistsException;
import com.kola.backend.exception.InsufficientCreditScoreException;
import com.kola.backend.exception.LoanNotEligibleException;
import com.kola.backend.notification.NotificationService;
import com.kola.backend.notification.NotificationType;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRequestRepository loanRequestRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final CreditScoringService creditScoringService;
    private final RepaymentCapacityService repaymentCapacityService;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final NotificationService notificationService;

    // Score minimum pour être éligible (correspond à BASIC tier, score ≥ 40)
    private static final int MIN_ELIGIBLE_SCORE = CreditTier.BASIC.getMinScore();

    /**
     * Conditions couperet, non compensables par le score.
     *
     * Elles repondent a ce qu'un bareme pondere ne sait pas exprimer : un
     * compte trop jeune n'a pas d'historique a lire, et un compte non verifie
     * n'a pas d'identite etablie. Aucun volume de transactions ne remplace
     * l'un ou l'autre.
     */
    private static final int MIN_ACCOUNT_AGE_DAYS = 90;

    /**
     * Au-delà de ce montant, un humain tranche.
     *
     * ═══ POURQUOI UN SEUIL PLUTÔT QUE « TOUT OU RIEN » ═══
     *
     * Faire examiner chaque prêt supprimerait l'intérêt d'un service instantané
     * pour des sommes modestes ; n'en examiner aucun revient à confier des
     * montants importants à un barème de points que personne ne surveille. Le
     * seuil place le curseur là où l'erreur coûte plus cher que l'attente.
     *
     * 200 000 XOF : au-dessus, la perte sèche dépasse ce que quelques minutes
     * d'examen coûtent. À recalibrer sur le taux de défaut observé, comme les
     * autres curseurs du module.
     */
    private static final BigDecimal MANUAL_REVIEW_THRESHOLD = new BigDecimal("200000");

    // ═══════════════════════════════════════════════════════════════
    //  DEMANDE DE PRÊT
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LoanDtos.LoanResponse apply(User borrower, LoanDtos.LoanApplicationRequest req) {

        // 1. Un seul prêt non soldé à la fois.
        //    Ce contrôle passe en PREMIER, avant les conditions d'accès : à
        //    quelqu'un qui a déjà un prêt en cours, « remboursez d'abord » est
        //    une réponse plus utile que « votre compte est trop récent ».
        if (loanRequestRepository.hasDefaultedLoan(borrower.getId())) {
            throw ActiveLoanExistsException.defaulted();
        }
        if (loanRequestRepository.hasActiveLoan(borrower.getId())) {
            throw new ActiveLoanExistsException();
        }

        // 2. Conditions couperet : elles ne se rachètent pas par un bon score.
        checkEligibility(borrower);

        // 3. Score de crédit (recalculé si périmé)
        ScoreBreakdown breakdown = creditScoringService.getOrCompute(borrower);
        if (breakdown.totalScore() < MIN_ELIGIBLE_SCORE) {
            throw new InsufficientCreditScoreException(breakdown.totalScore(), MIN_ELIGIBLE_SCORE);
        }

        CreditScore scoreSnapshot = creditScoreRepository
                .findByUserIdAndLatestTrue(borrower.getId())
                .orElseThrow();

        /* 3. Montant dans la limite de la CAPACITÉ, pas seulement du palier.
              C'est ici que deux emprunteurs de même score divergent : le palier
              dit la confiance et fixe un plafond absolu, la capacité lit les
              flux réels et fixe le montant. Le plafond de palier reste appliqué
              en amont par RepaymentCapacityService — inutile de le revérifier. */
        LoanCapacity capacity = repaymentCapacityService.compute(borrower, scoreSnapshot.getTier());
        BigDecimal maxForDuration = capacity.maxAmountFor(req.durationMonths());

        if (capacity.isEmpty()) {
            throw new LoanNotEligibleException(
                    "Votre activité des 90 derniers jours ne permet pas encore d'évaluer une "
                    + "capacité de remboursement. Utilisez votre compte régulièrement, puis réessayez.");
        }

        if (req.requestedAmount().compareTo(maxForDuration) > 0) {
            throw new IllegalArgumentException(
                    "Montant demandé (" + req.requestedAmount() + " XOF) supérieur à votre capacité "
                    + "de remboursement sur " + req.durationMonths() + " mois (" + maxForDuration
                    + " XOF). Facteur limitant : " + capacity.limitingFactor().getLabel() + ".");
        }

        // 4. Calcul du remboursement total : principal × (1 + taux × durée)
        BigDecimal totalRepayment = req.requestedAmount()
                .multiply(BigDecimal.ONE
                        .add(scoreSnapshot.getMonthlyRate()
                                .multiply(BigDecimal.valueOf(req.durationMonths()))))
                .setScale(2, RoundingMode.HALF_UP);

        // 5. Wallet de destination, VERROUILLÉ : le déboursement plus bas fait
        //    un balance += montant. Sans verrou pessimiste, un transfert
        //    concurrent lisait le solde d'avant et écrasait le crédit du prêt
        //    (lost update). findOwnedWalletForUpdateOrThrow refuse en prime un
        //    portefeuille suspendu, que findOwnedWalletOrThrow créditait
        //    volontiers.
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(borrower, req.walletId());

        /* 6. Automatique en dessous du seuil, examiné au-dessus.
              Un prêt en attente n'a PAS de date d'échéance : elle serait fausse
              dès que l'examen prend un jour. Elle est posée au déboursement,
              seul moment où l'argent existe réellement chez l'emprunteur. */
        boolean needsReview = req.requestedAmount().compareTo(MANUAL_REVIEW_THRESHOLD) > 0;

        LoanRequest loan = LoanRequest.builder()
                .borrower(borrower)
                .wallet(wallet)
                .creditScoreSnapshot(scoreSnapshot)
                .requestedAmount(req.requestedAmount())
                .durationMonths(req.durationMonths())
                .monthlyRate(scoreSnapshot.getMonthlyRate())
                .totalRepayment(totalRepayment)
                .status(needsReview ? LoanStatus.PENDING : LoanStatus.APPROVED)
                .purpose(req.purpose())
                .dueDate(needsReview ? null : LocalDate.now().plusMonths(req.durationMonths()))
                .build();

        loan = loanRequestRepository.save(loan);

        if (needsReview) {
            notificationService.notify(
                    borrower,
                    "Demande de prêt en cours d'examen",
                    "Votre demande de " + req.requestedAmount() + " XOF est en cours d'examen. "
                            + "Vous serez notifié dès qu'une décision sera prise.",
                    NotificationType.SYSTEM
            );
            log.info("Prêt #{} de user {} en attente d'examen : {} XOF (seuil {} XOF)",
                    loan.getId(), borrower.getId(), req.requestedAmount(), MANUAL_REVIEW_THRESHOLD);
        } else {
            disburseLoan(loan, borrower, wallet);
            log.info("Prêt #{} accordé à user {} : {} XOF sur {} mois",
                    loan.getId(), borrower.getId(), req.requestedAmount(), req.durationMonths());
        }

        return LoanDtos.LoanResponse.fromEntity(loan);
    }

    /**
     * Conditions d'accès au crédit, vérifiées avant tout calcul.
     *
     * Volontairement en premier : refuser après avoir calculé un score et une
     * capacité coûte des requêtes pour rien, et surtout le message d'erreur
     * doit désigner la vraie cause plutôt qu'un plafond à zéro dont
     * l'emprunteur ne saurait que faire.
     */
    private void checkEligibility(User borrower) {
        long accountAgeDays = borrower.getCreatedAt() == null
                ? 0
                : ChronoUnit.DAYS.between(borrower.getCreatedAt().toLocalDate(), LocalDate.now());

        if (accountAgeDays < MIN_ACCOUNT_AGE_DAYS) {
            throw new LoanNotEligibleException(
                    "Votre compte doit avoir au moins " + MIN_ACCOUNT_AGE_DAYS + " jours pour "
                    + "demander un prêt (actuellement " + accountAgeDays + " jours).");
        }

        /* Le niveau 0 est un compte dont l'identité n'a pas été vérifiée.
           Prêter dessus, c'est prêter à quelqu'un qu'on ne peut pas retrouver. */
        if (borrower.getKycLevel() == KycLevel.TIER_0) {
            throw new LoanNotEligibleException(
                    "Un niveau de vérification d'identité supérieur est nécessaire pour emprunter. "
                    + "Contactez le service client pour compléter votre vérification.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXAMEN MANUEL (au-delà du seuil)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Accorde un prêt en attente et le débourse.
     *
     * ═══ POURQUOI CETTE MÉTHODE VIT ICI ET NON DANS UN SERVICE D'ADMIN ═══
     *
     * Parce qu'accorder un prêt DÉPLACE DE L'ARGENT. L'invariant du module
     * admin — « aucun endpoint n'écrit directement le statut d'un prêt » —
     * tient toujours : le contrôleur d'administration appelle cette méthode,
     * qui crédite le wallet, écrit l'écriture au grand livre et notifie. Un
     * simple `setStatus(APPROVED)` dans un service d'admin aurait désynchronisé
     * le statut du prêt et le solde réel.
     *
     * L'ÉCHÉANCE EST CALCULÉE MAINTENANT, pas à la demande : l'emprunteur
     * dispose de sa durée pleine à partir du jour où il reçoit l'argent. La
     * compter depuis la demande lui ferait payer le délai d'examen.
     */
    @Transactional
    public LoanDtos.LoanResponse approve(User admin, Long loanId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Prêt introuvable : " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException(
                    "Seul un prêt en attente d'examen peut être accordé (statut actuel : "
                            + loan.getStatus() + ").");
        }

        User borrower = loan.getBorrower();
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(borrower, loan.getWallet().getId());

        loan.setDueDate(LocalDate.now().plusMonths(loan.getDurationMonths()));
        disburseLoan(loan, borrower, wallet);

        /* Traçabilité : même dispositif que les décisions KYC — une ligne WARN
           portant l'administrateur qui décide. Il n'existe pas encore de table
           d'audit ; c'est le chantier connu de ce module. */
        log.warn("DÉCISION CRÉDIT — prêt #{} ({} XOF) ACCORDÉ par {}",
                loan.getId(), loan.getRequestedAmount(), admin.getEmail());

        return LoanDtos.LoanResponse.fromEntity(loan);
    }

    /**
     * Refuse un prêt en attente.
     *
     * Le motif est OBLIGATOIRE et conservé sur le prêt : un refus sans raison
     * est incontestable par l'emprunteur et inexploitable par le support. Il
     * est repris tel quel dans la notification — c'est le même texte des deux
     * côtés, donc rien à recouper en cas de litige.
     */
    @Transactional
    public LoanDtos.LoanResponse reject(User admin, Long loanId, String reason) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Prêt introuvable : " + loanId));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException(
                    "Seul un prêt en attente d'examen peut être refusé (statut actuel : "
                            + loan.getStatus() + ").");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);
        loanRequestRepository.save(loan);

        notificationService.notify(
                loan.getBorrower(),
                "Demande de prêt refusée",
                "Votre demande de " + loan.getRequestedAmount() + " XOF n'a pas été retenue. Motif : " + reason,
                NotificationType.SYSTEM
        );

        log.warn("DÉCISION CRÉDIT — prêt #{} ({} XOF) REFUSÉ par {} — motif : {}",
                loan.getId(), loan.getRequestedAmount(), admin.getEmail(), reason);

        return LoanDtos.LoanResponse.fromEntity(loan);
    }

    /** Montant au-delà duquel une décision humaine est requise. */
    public static BigDecimal manualReviewThreshold() {
        return MANUAL_REVIEW_THRESHOLD;
    }

    // ═══════════════════════════════════════════════════════════════
    //  REMBOURSEMENT
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public LoanDtos.LoanResponse repay(User borrower, Long loanId) {
        LoanRequest loan = findOwnedLoanOrThrow(borrower, loanId);

        // DEFAULTED est accepté au même titre que DISBURSED : n'autoriser que
        // DISBURSED rendait une créance en défaut littéralement impossible à
        // régulariser. Le batch de 2h la faisait basculer, et l'emprunteur se
        // retrouvait avec une dette qu'il ne pouvait plus payer, sans aucune
        // voie de recouvrement côté Kola.
        if (loan.getStatus() != LoanStatus.DISBURSED && loan.getStatus() != LoanStatus.DEFAULTED) {
            throw new IllegalStateException(
                    "Ce prêt n'est pas en cours de remboursement (statut : " + loan.getStatus() + ")."
            );
        }

        // Débite le wallet du remboursement total
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(borrower, loan.getWallet().getId());

        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(loan.getTotalRepayment()) < 0) {
            throw new com.kola.backend.exception.InsufficientFundsException(
                    "Solde insuffisant pour rembourser : " + available + " XOF disponibles, "
                    + loan.getTotalRepayment() + " XOF requis."
            );
        }

        wallet.setBalance(wallet.getBalance().subtract(loan.getTotalRepayment()));

        Transaction repayTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.LOAN_REPAYMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(loan.getTotalRepayment())
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(borrower)
                .description("Remboursement prêt #" + loan.getId())
                .build();
        transactionRepository.save(repayTx);

        loan.setStatus(LoanStatus.REPAID);
        loanRequestRepository.save(loan);

        // La réponse est construite MAINTENANT, avant le recalcul du score.
        // computeAndSave() passe par markAllAsNotLatest(), annoté
        // @Modifying(clearAutomatically = true) : il vide le contexte de
        // persistance et détache `loan`. Or fromEntity() lit le proxy paresseux
        // creditScoreSnapshot → LazyInitializationException, et le rollback
        // annulait tout le remboursement (le prêt restait DISBURSED, sans que
        // l'utilisateur comprenne pourquoi).
        LoanDtos.LoanResponse response = LoanDtos.LoanResponse.fromEntity(loan);

        // Recalcul du score (le remboursement améliore le profil)
        creditScoringService.computeAndSave(borrower.getId());

        notificationService.notify(
                borrower,
                "Prêt remboursé",
                "Votre prêt de " + loan.getRequestedAmount() + " XOF a été intégralement remboursé.",
                NotificationType.TRANSACTION
        );

        log.info("Prêt #{} remboursé par user {}", loanId, borrower.getId());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<LoanDtos.LoanResponse> getMyLoans(User borrower) {
        return loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(borrower.getId())
                .stream()
                .map(LoanDtos.LoanResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanDtos.LoanResponse getLoan(User borrower, Long loanId) {
        return LoanDtos.LoanResponse.fromEntity(findOwnedLoanOrThrow(borrower, loanId));
    }

    // ═══════════════════════════════════════════════════════════════
    //  JOB BATCH : détection des impayés (appelé par le scheduler)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void markOverdueLoans() {
        List<LoanRequest> overdue = loanRequestRepository.findOverdueLoans();
        overdue.forEach(loan -> {
            loan.setStatus(LoanStatus.DEFAULTED);
            // Trace permanente : le statut redeviendra REPAID si l'emprunteur
            // régularise, mais le défaut doit rester visible du scoring.
            loan.setDefaultedAt(LocalDateTime.now());
            notificationService.notify(
                    loan.getBorrower(),
                    "Prêt en défaut de paiement",
                    "Votre prêt de " + loan.getRequestedAmount() + " XOF a dépassé son échéance du "
                            + loan.getDueDate() + ". Régularisez-le pour pouvoir emprunter à nouveau.",
                    NotificationType.SYSTEM
            );
            log.warn("Prêt #{} marqué DEFAULTED (user {})", loan.getId(), loan.getBorrower().getId());
        });
        loanRequestRepository.saveAll(overdue);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════════════════

    private void disburseLoan(LoanRequest loan, User borrower, Wallet wallet) {
        wallet.setBalance(wallet.getBalance().add(loan.getRequestedAmount()));

        Transaction disburseTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.LOAN_DISBURSEMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(loan.getRequestedAmount())
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(borrower)
                .description("Déboursement prêt #" + loan.getId())
                .build();
        transactionRepository.save(disburseTx);

        loan.setStatus(LoanStatus.DISBURSED);

        notificationService.notify(
                borrower,
                "Prêt accordé",
                "Votre prêt de " + loan.getRequestedAmount() + " XOF a été approuvé et versé sur votre wallet.",
                NotificationType.TRANSACTION
        );
    }

    private LoanRequest findOwnedLoanOrThrow(User borrower, Long loanId) {
        LoanRequest loan = loanRequestRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Prêt introuvable"));
        if (!loan.getBorrower().getId().equals(borrower.getId())) {
            throw new AccessDeniedException("Ce prêt ne vous appartient pas");
        }
        return loan;
    }
}
