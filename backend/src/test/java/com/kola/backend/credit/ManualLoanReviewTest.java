package com.kola.backend.credit;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Au-delà d'un certain montant, un humain tranche.
 *
 * ═══ CE QUE CES TESTS PROTÈGENT ═══
 *
 * Avant, `apply()` posait `status = APPROVED` et décaissait dans la foulée,
 * quel que soit le montant : le barème de points décidait seul de sommes qui
 * peuvent atteindre deux millions. Le point vérifié ici est le plus important
 * de tous — QUE L'ARGENT NE BOUGE PAS avant la décision. Un prêt en attente qui
 * créditerait quand même le wallet rendrait l'examen décoratif.
 */
@SpringBootTest
class ManualLoanReviewTest {

    @Autowired
    private LoanService loanService;
    @Autowired
    private LoanRequestRepository loanRequestRepository;
    @Autowired
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private CreditScoringService creditScoringService;
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

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(900);
    private final List<User> created = new ArrayList<>();

    private User emprunteur;
    private User administrateur;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        emprunteur = givenUser("emprunteur");
        administrateur = givenUser("admin");
        wallet = givenWallet(emprunteur);
    }

    @AfterEach
    void tearDown() {
        created.forEach(u -> {
            loanRequestRepository.deleteAll(
                    loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(u.getId()));
            creditScoreRepository.deleteAll(
                    creditScoreRepository.findByUserIdOrderByCreatedAtDesc(u.getId()));
            walletRepository.findByOwnerId(u.getId()).forEach(w ->
                    transactionRepository.deleteAll(
                            transactionRepository.findByWalletIdOrderByCreatedAtDesc(w.getId())));
        });
        notificationRepository.deleteAll(notificationRepository.findAll().stream()
                .filter(n -> created.stream().anyMatch(u -> u.getId().equals(n.getUser().getId())))
                .toList());
        created.forEach(u -> walletRepository.deleteAll(walletRepository.findByOwnerId(u.getId())));
        userRepository.deleteAll(created);
        created.clear();
    }

    @Test
    void leSeuilExisteEtEstConnuDeLApplication() {
        assertThat(LoanService.manualReviewThreshold())
                .as("le seuil doit être lisible de l'extérieur : la console en a besoin pour prévenir l'emprunteur")
                .isEqualByComparingTo("200000");
    }

    @Test
    void unPretAuDelaDuSeuilNeDeboursePasAvantDecision() {
        LoanRequest pret = givenPendingLoan("500000");

        assertThat(pret.getStatus())
                .as("au-delà du seuil, le prêt attend une décision humaine")
                .isEqualTo(LoanStatus.PENDING);
        assertThat(pret.getDueDate())
                .as("aucune échéance tant que l'argent n'est pas versé : elle serait fausse dès le lendemain")
                .isNull();
        assertThat(reloadWallet().getBalance())
                .as("LE POINT CRITIQUE : rien n'est crédité tant qu'un humain n'a pas tranché")
                .isEqualByComparingTo("0");
    }

    @Test
    void accorderUnPretEnAttenteLeDebourseEtPoseLEcheance() {
        LoanRequest pret = givenPendingLoan("500000");

        loanService.approve(administrateur, pret.getId());

        LoanRequest apres = reload(pret);
        assertThat(apres.getStatus()).isEqualTo(LoanStatus.DISBURSED);
        assertThat(reloadWallet().getBalance()).isEqualByComparingTo("500000");
        assertThat(apres.getDueDate())
                .as("la durée court à partir du versement, pas de la demande : "
                        + "le délai d'examen ne doit pas être payé par l'emprunteur")
                .isEqualTo(LocalDate.now().plusMonths(apres.getDurationMonths()));
    }

    @Test
    void refuserUnPretConserveLeMotifEtNeCreditePas() {
        LoanRequest pret = givenPendingLoan("500000");

        loanService.reject(administrateur, pret.getId(), "Revenus déclarés incohérents avec les flux observés");

        LoanRequest apres = reload(pret);
        assertThat(apres.getStatus()).isEqualTo(LoanStatus.REJECTED);
        assertThat(apres.getRejectionReason())
                .as("un refus sans motif est incontestable par l'emprunteur et inexploitable par le support")
                .contains("incohérents");
        assertThat(reloadWallet().getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void unPretDejaTrancheNeSeRejugePas() {
        LoanRequest pret = givenPendingLoan("500000");
        loanService.approve(administrateur, pret.getId());

        assertThatThrownBy(() -> loanService.approve(administrateur, pret.getId()))
                .as("un second accord déboursait une seconde fois : le solde doublait")
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> loanService.reject(administrateur, pret.getId(), "Changement d'avis tardif"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(reloadWallet().getBalance())
                .as("quoi qu'il arrive, un prêt ne se débourse qu'une fois")
                .isEqualByComparingTo("500000");
    }

    // ── Fixtures ───────────────────────────────────────────────────

    /**
     * Crée directement un prêt en attente.
     *
     * On ne passe pas par `apply()` : ses conditions d'accès (compte de 90
     * jours, capacité de remboursement observée) demanderaient de fabriquer
     * trois mois d'historique, ce que `RepaymentCapacityTest` couvre déjà. Ce
     * qui est vérifié ici est ce qui se passe APRÈS, une fois le prêt en
     * attente.
     */
    private LoanRequest givenPendingLoan(String amount) {
        CreditScore snapshot = creditScoringService.computeAndSave(emprunteur.getId());
        BigDecimal montant = new BigDecimal(amount);

        return loanRequestRepository.save(LoanRequest.builder()
                .borrower(emprunteur)
                .wallet(wallet)
                .creditScoreSnapshot(snapshot)
                .requestedAmount(montant)
                .durationMonths(6)
                .monthlyRate(new BigDecimal("0.015"))
                .totalRepayment(montant.multiply(new BigDecimal("1.09")))
                .status(LoanStatus.PENDING)
                .purpose("Test examen manuel")
                .dueDate(null)
                .build());
    }

    private LoanRequest reload(LoanRequest loan) {
        return loanRequestRepository.findById(loan.getId()).orElseThrow();
    }

    private Wallet reloadWallet() {
        return walletRepository.findById(wallet.getId()).orElseThrow();
    }

    private User givenUser(String prefix) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent"));

        User user = userRepository.save(User.builder()
                .firstName(prefix)
                .lastName("Test")
                .email("review-" + prefix + "-" + unique + "@kola.test")
                .phoneNumber("+22892" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
                .countryCode("TG")
                .kycLevel(KycLevel.TIER_2)
                .password(passwordEncoder.encode("Test1234"))
                .roles(List.of(clientRole))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build());
        created.add(user);
        return user;
    }

    private Wallet givenWallet(User owner) {
        return walletRepository.save(Wallet.builder()
                .currency("XOF")
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(owner)
                .build());
    }
}
