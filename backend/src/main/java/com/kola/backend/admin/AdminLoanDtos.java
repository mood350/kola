package com.kola.backend.admin;

import com.kola.backend.credit.CreditTier;
import com.kola.backend.credit.LoanRequest;
import com.kola.backend.credit.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vues administrateur du portefeuille de prêts.
 *
 * DIFFÉRENCE AVEC `LoanDtos.LoanResponse`, LE DTO CLIENT : celui-ci est toujours
 * lu par l'emprunteur lui-même, qui sait qui il est — inutile de lui répéter son
 * identité. L'administration regarde la population entière : sans emprunteur
 * sur chaque ligne, une liste de prêts n'est qu'une colonne de montants.
 *
 * S'y ajoutent deux champs que le client n'a pas à voir : `defaultedAt`, la
 * trace d'un défaut passé conservée même après régularisation, et `createdAt`,
 * qui donne l'ancienneté du dossier.
 */
public final class AdminLoanDtos {

    private AdminLoanDtos() {}

    public record AdminLoanSummary(
            Long id,
            Long borrowerId,
            String borrowerFullName,
            String borrowerEmail,
            BigDecimal requestedAmount,
            BigDecimal totalRepayment,
            BigDecimal monthlyRate,
            int durationMonths,
            LoanStatus status,
            String purpose,
            LocalDate dueDate,
            String rejectionReason,
            int creditScoreAtRequest,
            CreditTier tierAtRequest,
            LocalDateTime defaultedAt,
            LocalDateTime createdAt
    ) {
        /**
         * À N'APPELER QUE SUR UNE ENTITÉ CHARGÉE AVEC SES JOINTURES
         * (`findAllForAdmin`, `findByStatusForAdmin`, `findByIdForAdmin`).
         * `borrower` et `creditScoreSnapshot` sont LAZY : sur une entité issue
         * d'un `findById` nu, et hors transaction, ces accès échouent.
         */
        public static AdminLoanSummary fromEntity(LoanRequest loan) {
            var borrower = loan.getBorrower();
            var snapshot = loan.getCreditScoreSnapshot();

            return new AdminLoanSummary(
                    loan.getId(),
                    borrower != null ? borrower.getId() : null,
                    borrower != null ? borrower.fullName() : null,
                    borrower != null ? borrower.getEmail() : null,
                    loan.getRequestedAmount(),
                    loan.getTotalRepayment(),
                    loan.getMonthlyRate(),
                    loan.getDurationMonths(),
                    loan.getStatus(),
                    loan.getPurpose(),
                    loan.getDueDate(),
                    loan.getRejectionReason(),
                    snapshot != null ? snapshot.getScore() : 0,
                    snapshot != null ? snapshot.getTier() : null,
                    loan.getDefaultedAt(),
                    loan.getCreatedAt()
            );
        }
    }

    /** Une ligne de la ventilation par statut. */
    public record LoanStatusBucket(
            LoanStatus status,
            long count,
            BigDecimal principal,
            BigDecimal totalRepayment
    ) {}

    /**
     * Volumétrie du portefeuille.
     *
     * `outstandingPrincipal` ne compte que les statuts APPROVED, DISBURSED et
     * DEFAULTED — les prêts dont l'argent est effectivement engagé ou dû. Y
     * inclure les REPAID gonflerait l'encours de tout ce qui a déjà été
     * remboursé, et les REJECTED d'argent qui n'est jamais sorti.
     */
    public record AdminLoanOverview(
            long total,
            BigDecimal outstandingPrincipal,
            BigDecimal outstandingRepayment,
            List<LoanStatusBucket> byStatus
    ) {}

    /**
     * Motif d'un refus de prêt.
     *
     * Longueur imposée comme pour les décisions KYC : « non » ne dit rien à
     * l'emprunteur, et 500 caractères suffisent largement à une décision de
     * crédit. Cette contrainte est ce qui rend le refus contestable — donc
     * défendable.
     */
    public record RejectLoanRequest(
            @jakarta.validation.constraints.NotBlank(message = "Le motif est obligatoire")
            @jakarta.validation.constraints.Size(
                    min = 10, max = 500,
                    message = "Le motif doit contenir entre 10 et 500 caractères")
            String reason
    ) {}
}
