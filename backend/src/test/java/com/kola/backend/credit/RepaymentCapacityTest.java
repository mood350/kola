package com.kola.backend.credit;

import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le montant empruntable suit l'usage du compte, pas seulement le score.
 *
 * ═══ CE QUE CES TESTS FIGENT ═══
 *
 * La règle demandée : deux personnes au MÊME palier de confiance n'empruntent
 * pas la même somme si leurs flux diffèrent. Avant ce module, elles recevaient
 * toutes deux le plafond de leur palier — un montant décidé par un barème de
 * points, sans aucun rapport avec ce qu'elles encaissent réellement.
 *
 * Chaque test ci-dessous correspond à un profil que le plafond par palier
 * traitait à tort de façon identique.
 */
@SpringBootTest
class RepaymentCapacityTest {

    @Autowired
    private RepaymentCapacityService capacityService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private LoanRequestRepository loanRequestRepository;
    @Autowired
    private CreditScoringService creditScoringService;
    @Autowired
    private CreditScoreRepository creditScoreRepository;

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(600);
    private final List<User> created = new ArrayList<>();

    @AfterEach
    void tearDown() {
        /* Ordre imposé par les clés étrangères : les prêts et les scores
           référencent le wallet et l'utilisateur, ils partent en premier. */
        created.forEach(u -> {
            loanRequestRepository.deleteAll(
                    loanRequestRepository.findByBorrowerIdOrderByCreatedAtDesc(u.getId()));
            creditScoreRepository.deleteAll(
                    creditScoreRepository.findByUserIdOrderByCreatedAtDesc(u.getId()));
        });
        created.forEach(u -> walletRepository.findByOwnerId(u.getId()).forEach(w ->
                transactionRepository.deleteAll(
                        transactionRepository.findByWalletIdOrderByCreatedAtDesc(w.getId()))));
        created.forEach(u -> walletRepository.deleteAll(walletRepository.findByOwnerId(u.getId())));
        userRepository.deleteAll(created);
        created.clear();
    }

    // ═══════════════════════════════════════════════════════════════
    //  LE CAS DEMANDÉ : même score, flux différents, montants différents
    // ═══════════════════════════════════════════════════════════════

    @Test
    void aPalierEgalDeuxProfilsDeFluxNObtiennentPasLeMemeMontant() {
        User modeste = givenUserWithFlows("modeste", "20000", "5000");
        User actif = givenUserWithFlows("actif", "300000", "50000");

        // MÊME palier : la confiance est identique, seuls les flux changent.
        LoanCapacity capaciteModeste = capacityService.compute(modeste, CreditTier.PREMIUM);
        LoanCapacity capaciteActif = capacityService.compute(actif, CreditTier.PREMIUM);

        assertThat(capaciteActif.maxAmountFor(6))
                .as("celui qui encaisse quinze fois plus doit pouvoir emprunter davantage, à palier égal")
                .isGreaterThan(capaciteModeste.maxAmountFor(6));

        assertThat(capaciteModeste.maxAmountFor(6))
                .as("et celui qui encaisse peu ne doit pas se voir offrir le plafond du palier")
                .isLessThan(CreditTier.PREMIUM.getMaxLoanAmount());
    }

    @Test
    void unGrosChiffreDAffairesNeVautPasUneGrosseCapacite() {
        // Le commerçant : 500 000 encaissés, 495 000 dépensés. Un plafond fondé
        // sur le volume lui prêterait une fortune ; il ne dispose que de 5 000.
        User commercant = givenUserWithFlows("commercant", "500000", "495000");
        User epargnant = givenUserWithFlows("epargnant", "120000", "20000");

        LoanCapacity capaciteCommercant = capacityService.compute(commercant, CreditTier.PREMIUM);
        LoanCapacity capaciteEpargnant = capacityService.compute(epargnant, CreditTier.PREMIUM);

        assertThat(capaciteEpargnant.maxAmountFor(6))
                .as("ce qui compte est le disponible, pas le chiffre d'affaires")
                .isGreaterThan(capaciteCommercant.maxAmountFor(6));
    }

    @Test
    void desRevenusIrreguliersSontDecotes() {
        // Même total encaissé, mais l'un l'a reçu sur trois mois et l'autre en
        // une fois. Une moyenne mensuelle seule ne les distingue pas.
        /* Montants volontairement petits : au-delà, le plafond de progression
           (50 000 pour un premier prêt) écraserait les deux profils à la même
           valeur et le test ne mesurerait plus la décote qu'il vise. */
        User regulier = givenUser("regulier");
        Wallet walletRegulier = givenWallet(regulier);
        depot(regulier, walletRegulier, "10000", 75);
        depot(regulier, walletRegulier, "10000", 45);
        depot(regulier, walletRegulier, "10000", 10);

        User ponctuel = givenUser("ponctuel");
        Wallet walletPonctuel = givenWallet(ponctuel);
        depot(ponctuel, walletPonctuel, "30000", 10);

        LoanCapacity capaciteReguliere = capacityService.compute(regulier, CreditTier.PREMIUM);
        LoanCapacity capacitePonctuelle = capacityService.compute(ponctuel, CreditTier.PREMIUM);

        assertThat(capaciteReguliere.activeMonths()).isEqualTo(3);
        assertThat(capacitePonctuelle.activeMonths()).isEqualTo(1);
        assertThat(capaciteReguliere.maxAmountFor(6))
                .as("un revenu récurrent se rembourse mieux qu'une rentrée isolée du même montant")
                .isGreaterThan(capacitePonctuelle.maxAmountFor(6));
    }

    // ═══════════════════════════════════════════════════════════════
    //  LES BORNES
    // ═══════════════════════════════════════════════════════════════

    @Test
    void lePremierPretResteModesteMemeAvecDeGrosFlux() {
        User fortune = givenUserWithFlows("fortune", "2000000", "100000");

        LoanCapacity capacity = capacityService.compute(fortune, CreditTier.ELITE);

        assertThat(capacity.maxAmountFor(12))
                .as("sans aucun prêt remboursé, le plafond de progression s'applique")
                .isLessThanOrEqualTo(new BigDecimal("50000"));
        assertThat(capacity.limitingFactor())
                .isEqualTo(LoanCapacity.LimitingFactor.GRADUATION);
    }

    @Test
    void unCompteSansActiviteNObtientAucuneCapacite() {
        User inactif = givenUser("inactif");
        givenWallet(inactif);

        LoanCapacity capacity = capacityService.compute(inactif, CreditTier.ELITE);

        assertThat(capacity.isEmpty())
                .as("aucun flux observé : rien sur quoi fonder un prêt, quel que soit le palier")
                .isTrue();
        assertThat(capacity.limitingFactor())
                .isEqualTo(LoanCapacity.LimitingFactor.NO_ACTIVITY);
    }

    @Test
    void uneDureePlusLongueAugmenteLeMontantEmpruntable() {
        // Flux modestes, pour que la capacité reste sous le plafond de
        // progression : c'est l'effet de la durée qu'on veut isoler.
        User client = givenUserWithFlows("duree", "10000", "2000");

        LoanCapacity capacity = capacityService.compute(client, CreditTier.PREMIUM);

        assertThat(capacity.maxAmountFor(12))
                .as("le prêt se solde à l'échéance : plus la durée est longue, plus l'accumulation est possible")
                .isGreaterThan(capacity.maxAmountFor(3));
    }

    @Test
    void leDisponibleEstCalculeSurTroisMois() {
        User client = givenUserWithFlows("moyenne", "90000", "30000");

        LoanCapacity capacity = capacityService.compute(client, CreditTier.PREMIUM);

        // 3 dépôts de 90 000 sur 3 mois = 90 000/mois ; 3 retraits de 30 000 = 30 000/mois.
        assertThat(capacity.monthlyInflow()).isEqualByComparingTo("90000");
        assertThat(capacity.monthlyOutflow()).isEqualByComparingTo("30000");
        assertThat(capacity.monthlyDisposable()).isEqualByComparingTo("60000");
    }

    @Test
    void rembourserUnPretLeveLePlafondDeProgression() {
        User client = givenUserWithFlows("gradue", "300000", "50000");

        LoanCapacity avant = capacityService.compute(client, CreditTier.PREMIUM);
        assertThat(avant.graduationCeiling()).isEqualByComparingTo("50000");

        givenRepaidLoan(client, "50000");

        LoanCapacity apres = capacityService.compute(client, CreditTier.PREMIUM);

        assertThat(apres.graduationCeiling())
                .as("chaque prêt remboursé à l'heure double le plafond suivant : "
                        + "l'emprunteur construit son accès au crédit")
                .isEqualByComparingTo("100000");
        assertThat(apres.maxAmountFor(6))
                .as("et le montant réellement empruntable suit")
                .isGreaterThan(avant.maxAmountFor(6));
    }

    // ── Fixtures ───────────────────────────────────────────────────

    /** Trois mois d'activité régulière : un dépôt et un retrait par mois. */
    private User givenUserWithFlows(String prefix, String monthlyIn, String monthlyOut) {
        User user = givenUser(prefix);
        Wallet wallet = givenWallet(user);
        for (int daysAgo : new int[]{75, 45, 10}) {
            depot(user, wallet, monthlyIn, daysAgo);
            if (new BigDecimal(monthlyOut).signum() > 0) {
                retrait(user, wallet, monthlyOut, daysAgo);
            }
        }
        return user;
    }

    private void depot(User user, Wallet wallet, String amount, int daysAgo) {
        mouvement(user, wallet, amount, daysAgo, TransactionType.DEPOSIT);
    }

    private void retrait(User user, Wallet wallet, String amount, int daysAgo) {
        mouvement(user, wallet, amount, daysAgo, TransactionType.WITHDRAWAL);
    }

    /**
     * Écrit un mouvement daté dans le passé.
     *
     * `createdAt` est posé par l'audit Spring Data à l'insertion et la colonne
     * est `updatable = false` : on la repositionne donc en SQL natif. C'est le
     * seul moyen de simuler un historique, et sans historique ces tests ne
     * testeraient rien.
     */
    void mouvement(User user, Wallet wallet, String amount, int daysAgo, TransactionType type) {
        Transaction tx = transactionRepository.save(Transaction.builder()
                .reference("CAP-" + UUID.randomUUID().toString().substring(0, 12))
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .amount(new BigDecimal(amount))
                .fee(BigDecimal.ZERO)
                .currency("XOF")
                .wallet(wallet)
                .sender(user)
                .description("Test capacité")
                .build());

        /* TransactionTemplate et non @Transactional : annoter une méthode
           appelée depuis la même classe ne passe par aucun proxy Spring, donc
           n'ouvre aucune transaction — et un UPDATE natif sans transaction
           échoue. */
        transactionTemplate.executeWithoutResult(status ->
                entityManager.createNativeQuery(
                                "update transactions set created_at = ?1 where id = ?2")
                        .setParameter(1, LocalDateTime.now().minusDays(daysAgo))
                        .setParameter(2, tx.getId())
                        .executeUpdate());
    }

    /** Un prêt soldé, pour vérifier la progression du plafond. */
    private void givenRepaidLoan(User borrower, String amount) {
        CreditScore snapshot = creditScoringService.computeAndSave(borrower.getId());
        Wallet wallet = walletRepository.findByOwnerId(borrower.getId()).get(0);

        loanRequestRepository.save(LoanRequest.builder()
                .borrower(borrower)
                .wallet(wallet)
                .creditScoreSnapshot(snapshot)
                .requestedAmount(new BigDecimal(amount))
                .durationMonths(3)
                .monthlyRate(new BigDecimal("0.015"))
                .totalRepayment(new BigDecimal(amount).multiply(new BigDecimal("1.045")))
                .status(LoanStatus.REPAID)
                .purpose("Test progression")
                .dueDate(java.time.LocalDate.now().minusDays(1))
                .build());
    }

    private User givenUser(String prefix) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent"));

        User user = userRepository.save(User.builder()
                .firstName(prefix)
                .lastName("Test")
                .email("cap-" + prefix + "-" + unique + "@kola.test")
                .phoneNumber("+22893" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
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
                .balance(new BigDecimal("100000"))
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(owner)
                .build());
    }
}
