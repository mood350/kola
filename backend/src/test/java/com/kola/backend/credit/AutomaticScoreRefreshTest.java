package com.kola.backend.credit;

import com.kola.backend.notification.NotificationRepository;
import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.transaction.DepositRequest;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le score se tient à jour tout seul.
 *
 * ═══ CE QUE CES TESTS REMPLACENT ═══
 *
 * Un bouton « Recalculer », présent dans les deux applications. Il faisait
 * porter à l'utilisateur une mécanique interne — deviner que sa note était
 * périmée, et penser à l'actionner. Deux automatismes le remplacent, et ce
 * sont eux qui sont vérifiés ici : la consultation recalcule, et une
 * transaction validée rafraîchit sans que personne n'ouvre l'écran.
 *
 * ═══ LE PIÈGE QUE LE TROISIÈME TEST GARDE ═══
 *
 * Recalculer à chaque lecture, écrit naïvement, insère une ligne par
 * consultation. `credit_scores` alimente `GET /credit/score/history`, que l'on
 * doit pouvoir relire lors d'un litige : trois ouvertures de page par jour le
 * rendraient illisible en une semaine. Une ligne ne s'écrit donc que si la
 * valeur a bougé.
 */
@SpringBootTest
class AutomaticScoreRefreshTest {

    @Autowired
    private CreditScoringService creditScoringService;
    @Autowired
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private TransactionService transactionService;
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

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(100);
    private final List<User> created = new ArrayList<>();

    private User client;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        client = givenUser();
        wallet = givenWallet(client);
    }

    @AfterEach
    void tearDown() {
        created.forEach(u -> {
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
    void laPremiereConsultationCalculeLeScore() {
        assertThat(scoresEnBase()).isZero();

        ScoreBreakdown breakdown = creditScoringService.getFresh(client);

        assertThat(breakdown.totalScore()).isNotNegative();
        assertThat(scoresEnBase())
                .as("un compte sans score doit en obtenir un dès la première ouverture de l'écran")
                .isEqualTo(1);
    }

    @Test
    void deuxConsultationsSansChangementNEcriventQuUneSeuleLigne() {
        creditScoringService.getFresh(client);
        creditScoringService.getFresh(client);
        creditScoringService.getFresh(client);

        assertThat(scoresEnBase())
                .as("recalculer à chaque lecture ne doit pas remplir l'historique de doublons : "
                        + "il enregistre les CHANGEMENTS de score, pas les consultations")
                .isEqualTo(1);
    }

    @Test
    void unDepotRafraichitLeScoreSansQuePersonneNOuvreLEcran() {
        int avant = creditScoringService.getFresh(client).totalScore();

        /* Un dépôt déplace trois règles à la fois : régularité, volume, et
           ratio dépenses/revenus (qui passe de « aucun revenu » à excellent).
           Le score ne peut pas rester identique. */
        transactionService.deposit(client, new DepositRequest(
                wallet.getId(), new BigDecimal("50000"), null, UUID.randomUUID().toString()));

        CreditScore enBase = creditScoreRepository
                .findByUserIdAndLatestTrue(client.getId())
                .orElseThrow();

        assertThat(enBase.getScore())
                .as("l'écouteur post-commit recalcule : la note stockée est à jour "
                        + "AVANT que l'utilisateur ne revienne sur l'écran Crédit")
                .isGreaterThan(avant);
        assertThat(scoresEnBase())
                .as("une valeur qui change, c'est une ligne de plus — et une seule")
                .isEqualTo(2);
    }

    @Test
    void leScoreLuApresUnDepotEstCeluiQuiEstEnBase() {
        creditScoringService.getFresh(client);
        transactionService.deposit(client, new DepositRequest(
                wallet.getId(), new BigDecimal("50000"), null, UUID.randomUUID().toString()));

        int apresDepot = creditScoringService.getFresh(client).totalScore();
        int stocke = creditScoreRepository.findByUserIdAndLatestTrue(client.getId())
                .orElseThrow().getScore();

        assertThat(apresDepot).isEqualTo(stocke);
        assertThat(scoresEnBase())
                .as("la consultation qui suit le dépôt ne doit rien réécrire : "
                        + "l'écouteur a déjà enregistré la nouvelle valeur")
                .isEqualTo(2);
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private long scoresEnBase() {
        return creditScoreRepository.findByUserIdOrderByCreatedAtDesc(client.getId()).size();
    }

    private User givenUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent"));

        User user = userRepository.save(User.builder()
                .firstName("Auto")
                .lastName("Score")
                .email("auto-" + unique + "@kola.test")
                .phoneNumber("+22891" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
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
