package com.kola.backend.credit;

import com.kola.backend.user.User;
import com.kola.backend.utils.Listeners;
import com.kola.backend.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Demande de prêt in-app Kola.
 *
 * RÈGLES MÉTIER :
 * - Un seul prêt ACTIF (APPROVED ou DISBURSED) par utilisateur à la fois.
 * - Le montant demandé ne peut pas dépasser CreditScore.maxLoanAmount.
 * - Le prêt est versé directement sur le wallet XOF de l'emprunteur.
 * - Le remboursement se fait en une seule fois (prêt bullet) :
 *   principal + intérêts au bout de durationMonths mois.
 */
@Entity
@Table(name = "loan_requests", indexes = {
        // hasActiveLoan, évalué à chaque demande
        @Index(name = "idx_loan_borrower_status", columnList = "borrower_id, status"),
        // findOverdueLoans, balayage du batch quotidien de mise en défaut
        @Index(name = "idx_loan_status_due", columnList = "status, dueDate")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRequest extends Listeners {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private User borrower;

    // Wallet sur lequel les fonds seront déboursés
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Score de crédit au moment de la demande (snapshot)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_score_id", nullable = false)
    private CreditScore creditScoreSnapshot;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    // Durée du prêt en mois (1 à 12)
    @Column(nullable = false)
    private int durationMonths;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal monthlyRate;

    // Montant total à rembourser = principal × (1 + taux × durée)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRepayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    // Motif de la demande (commerce, urgence médicale, etc.)
    private String purpose;

    // Date d'échéance de remboursement
    private LocalDate dueDate;

    // Raison du rejet éventuel
    private String rejectionReason;

    /**
     * Horodatage du passage en défaut, conservé même après régularisation.
     *
     * Le statut seul ne suffit pas : un emprunteur qui rembourse après coup
     * repasse en REPAID, et son défaut disparaîtrait de l'historique. Le score
     * de crédit blanchirait alors exactement le comportement qu'il est censé
     * pénaliser. Ce champ garde la trace ; il est nul pour un prêt sain.
     */
    private LocalDateTime defaultedAt;
}
