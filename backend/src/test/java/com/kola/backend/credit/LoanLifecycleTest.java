package com.kola.backend.credit;

import com.kola.backend.exception.ActiveLoanExistsException;
import com.kola.backend.notification.NotificationRepository;
import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cycle de vie d'un prêt en défaut.
 *
 * Zone jusqu'ici totalement dépourvue de tests, alors qu'elle contenait une
 * impasse : le batch nocturne faisait basculer un prêt échu en DEFAULTED, et
 * repay() n'acceptait que DISBURSED — la dette devenait impayable.
 */
@SpringBootTest
class LoanLifecycleTest {

    @Autowired
    private LoanService loanService;
    @Autowired
    private CreditScoringService creditScoringService;
    @Autowired
    private LoanRequestRepository loanRequestRepository;
    @Autowired
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(500);

    private User emprunteur;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        emprunteur = userRepository.save(User.builder()
                .firstName("Emprunteur")
                .lastName("Test")
                .email("loan-" + unique + "@kola.test")
                .phoneNumber("+22896" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
                .countryCode("TG")
                .kycLevel(KycLevel.TIER_2)
                .password(passwordEncoder.encode("Test1234"))
                .roles(List.of(clientRole))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build());

        wallet = walletRepository.save(Wallet.builder()
                .currency("XOF")
                .balance(new BigDecimal("50000"))
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(emprunteur)
                .build());
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll(
                transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()));
        loanRequestRepository.deleteAll(
                loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(emprunteur.getId()));
        creditScoreRepository.deleteAll(
                creditScoreRepository.findByUserIdOrderByCreatedAtDesc(emprunteur.getId()));
        notificationRepository.deleteAll(notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(emprunteur.getId()))
                .toList());
        // Rechargé et non supprimé depuis l'instance du champ : le prêt a
        // modifié ce portefeuille, donc sa version a changé. Supprimer la
        // copie périmée déclenche une ObjectOptimisticLockingFailureException
        // — le verrouillage optimiste faisant exactement son travail.
        walletRepository.findById(wallet.getId()).ifPresent(walletRepository::delete);
        userRepository.delete(emprunteur);
    }

    @Test
    void unPretEnDefautPeutEtreRegularise() {
        LoanRequest pret = givenOverdueDisbursedLoan();
        loanService.markOverdueLoans();

        assertThat(reload(pret).getStatus()).isEqualTo(LoanStatus.DEFAULTED);

        loanService.repay(emprunteur, pret.getId());

        assertThat(reload(pret).getStatus())
                .as("un prêt en défaut doit rester remboursable, sinon la créance est gelée à vie")
                .isEqualTo(LoanStatus.REPAID);
        assertThat(walletRepository.findById(wallet.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("39000")); // 50 000 − 11 000
    }

    @Test
    void unEmprunteurEnDefautNePeutPasSouscrireUnNouveauPret() {
        givenOverdueDisbursedLoan();
        loanService.markOverdueLoans();

        assertThatThrownBy(() -> loanService.apply(emprunteur,
                new LoanDtos.LoanApplicationRequest(wallet.getId(), new BigDecimal("5000"), 3, "Test")))
                .isInstanceOf(ActiveLoanExistsException.class)
                .hasMessageContaining("défaut de paiement");
    }

    @Test
    void leDefautResteVisibleDuScoreApresRegularisation() {
        LoanRequest pret = givenOverdueDisbursedLoan();
        loanService.markOverdueLoans();
        loanService.repay(emprunteur, pret.getId());

        assertThat(reload(pret).getDefaultedAt())
                .as("la trace du défaut ne doit pas être effacée par le remboursement")
                .isNotNull();

        creditScoringService.computeAndSave(emprunteur.getId());
        ScoreBreakdown breakdown = creditScoringService.getOrCompute(emprunteur);

        ScoreBreakdown.RuleScore historique = breakdown.details().stream()
                .filter(r -> r.rule() == ScoringRule.LOAN_REPAYMENT_HISTORY)
                .findFirst()
                .orElseThrow();

        assertThat(historique.points())
                .as("un défaut passé doit peser sur le score même une fois régularisé")
                .isZero();
    }

    @Test
    void leScoreEstNeutrePourUnEmprunteurSansHistorique() {
        ScoreBreakdown breakdown = creditScoringService.getOrCompute(emprunteur);

        ScoreBreakdown.RuleScore historique = breakdown.details().stream()
                .filter(r -> r.rule() == ScoringRule.LOAN_REPAYMENT_HISTORY)
                .findFirst()
                .orElseThrow();

        assertThat(historique.points())
                .as("ne jamais avoir emprunté est une absence de signal, pas un mauvais signal — "
                        + "mais 8/20 plafonne le score total à 88, donc PREMIUM au mieux : "
                        + "le palier ÉLITE se mérite en remboursant")
                .isEqualTo(8);
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private LoanRequest givenOverdueDisbursedLoan() {
        CreditScore snapshot = creditScoringService.computeAndSave(emprunteur.getId());

        return loanRequestRepository.save(LoanRequest.builder()
                .borrower(emprunteur)
                .wallet(wallet)
                .creditScoreSnapshot(snapshot)
                .requestedAmount(new BigDecimal("10000"))
                .durationMonths(1)
                .monthlyRate(new BigDecimal("0.100"))
                .totalRepayment(new BigDecimal("11000"))
                .status(LoanStatus.DISBURSED)
                .purpose("Test")
                .dueDate(LocalDate.now().minusDays(5))
                .build());
    }

    private LoanRequest reload(LoanRequest loan) {
        return loanRequestRepository.findById(loan.getId()).orElseThrow();
    }
}
