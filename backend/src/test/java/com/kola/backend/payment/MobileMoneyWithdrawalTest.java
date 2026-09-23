package com.kola.backend.payment;

import com.kola.backend.credit.CreditScoreRepository;
import com.kola.backend.notification.NotificationRepository;
import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionService;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Cycle de vie d'un retrait Mobile Money, du débit à l'issue du versement.
 *
 * ═══ CE QUI SE JOUE ICI, ET QUI DIFFÈRE DU DÉPÔT ═══
 *
 * Un dépôt qui échoue ne laisse rien : le portefeuille n'avait pas été crédité.
 * UN RETRAIT QUI ÉCHOUE LAISSE UN CLIENT AMPUTÉ. Le débit est immédiat — sinon
 * la somme resterait dépensable pendant que l'opérateur la traite, et le solde
 * afficherait un argent déjà parti — donc chaque chemin d'échec DOIT rendre le
 * montant ET les frais. C'est la propriété que ces tests tiennent : après
 * n'importe quelle issue négative, le solde est exactement celui d'avant.
 *
 * Le prestataire n'est pas appelé : ces tests portent sur le grand livre, pas
 * sur le transport HTTP.
 */
@SpringBootTest
class MobileMoneyWithdrawalTest {

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
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(700);
    private final List<User> created = new ArrayList<>();

    private User client;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        client = givenUser();
        wallet = givenWallet(client, "100000");
    }

    @AfterEach
    void tearDown() {
        created.forEach(u -> creditScoreRepository.deleteAll(
                creditScoreRepository.findByUserIdOrderByCreatedAtDesc(u.getId())));
        created.forEach(u -> walletRepository.findByOwnerId(u.getId()).forEach(w ->
                transactionRepository.deleteAll(
                        transactionRepository.findByWalletIdOrderByCreatedAtDesc(w.getId()))));
        notificationRepository.deleteAll(notificationRepository.findAll().stream()
                .filter(n -> created.stream().anyMatch(u -> u.getId().equals(n.getUser().getId())))
                .toList());
        created.forEach(u -> walletRepository.deleteAll(walletRepository.findByOwnerId(u.getId())));
        userRepository.deleteAll(created);
        created.clear();
    }

    @Test
    @DisplayName("Le débit est immédiat : la somme ne doit pas rester dépensable pendant le versement")
    void ouvrirUnRetraitDebiteToutDeSuite() {
        Transaction pending = ouvrirRetrait("10000");
        BigDecimal fee = pending.getFee();

        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(reload().getBalance())
                .as("montant ET frais quittent le solde dès l'ouverture")
                .isEqualByComparingTo(new BigDecimal("90000").subtract(fee));
    }

    @Test
    @DisplayName("Versement parti : le retrait est soldé, rien n'est rendu")
    void unVersementEnvoyeSoldeLeRetrait() {
        Transaction pending = ouvrirRetrait("10000");
        BigDecimal apresDebit = reload().getBalance();
        String payoutId = attacher(pending);

        transactionService.settleMobileMoneyWithdrawal(payoutId, true);

        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(reload().getBalance())
                .as("l'argent est arrivé chez l'opérateur : il ne revient pas")
                .isEqualByComparingTo(apresDebit);
    }

    @Test
    @DisplayName("Versement refusé : le montant ET les frais reviennent")
    void unVersementEchoueRendToutYComprisLesFrais() {
        Transaction pending = ouvrirRetrait("10000");
        String payoutId = attacher(pending);

        transactionService.settleMobileMoneyWithdrawal(payoutId, false);

        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.FAILED);
        assertThat(reload().getBalance())
                .as("les frais rémunèrent un service qui n'a pas été rendu")
                .isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Rapprochement rejoué : un échec ne recrédite qu'une fois")
    void unEchecRejoueNeRecrediteQuUneFois() {
        Transaction pending = ouvrirRetrait("25000");
        String payoutId = attacher(pending);

        /* La réconciliation repasse toutes les cinq minutes et peut relire le
           même versement : sans la garde de statut, chaque passage rendrait à
           nouveau l'argent — soit exactement de la création monétaire. */
        transactionService.settleMobileMoneyWithdrawal(payoutId, false);
        transactionService.settleMobileMoneyWithdrawal(payoutId, false);
        transactionService.settleMobileMoneyWithdrawal(payoutId, false);

        assertThat(reload().getBalance()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Un versement déjà soldé ne se rouvre pas")
    void unEchecApresUnSuccesNeRendRien() {
        Transaction pending = ouvrirRetrait("10000");
        BigDecimal apresDebit = reload().getBalance();
        String payoutId = attacher(pending);

        transactionService.settleMobileMoneyWithdrawal(payoutId, true);
        transactionService.settleMobileMoneyWithdrawal(payoutId, false);

        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(reload().getBalance())
                .as("recréditer un retrait déjà parti fabriquerait de l'argent")
                .isEqualByComparingTo(apresDebit);
    }

    @Test
    @DisplayName("Prestataire injoignable : l'abandon rend l'argent avant que l'erreur ne remonte")
    void unRetraitAbandonneEstRembourse() {
        Transaction pending = ouvrirRetrait("15000");

        transactionService.abandonPendingWithdrawal(pending.getId());

        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.FAILED);
        assertThat(reload().getBalance())
                .as("rien n'est parti : le portefeuille retrouve son solde exact")
                .isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Une issue portant sur un versement inconnu est ignorée sans casse")
    void uneIssueInconnueNeToucheAucunSolde() {
        transactionService.settleMobileMoneyWithdrawal("payout-qui-nexiste-pas", false);

        assertThat(reload().getBalance()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Rejeu d'une clé d'idempotence : un seul débit, une seule écriture")
    void rejouerLaMemeCleNeDebiteQuUneFois() {
        String cle = UUID.randomUUID().toString();

        Transaction premier = ouvrirRetrait("10000", cle);
        BigDecimal apresDebit = reload().getBalance();
        Transaction second = ouvrirRetrait("10000", cle);

        assertThat(second.getId()).isEqualTo(premier.getId());
        assertThat(reload().getBalance())
                .as("une requête renvoyée après une réponse perdue ne débite pas deux fois")
                .isEqualByComparingTo(apresDebit);
    }

    @Test
    @DisplayName("Le rapprochement ne voit que les retraits encore en attente")
    void seulsLesRetraitsEnAttenteSontARapprocher() {
        Transaction pending = ouvrirRetrait("10000");
        String payoutId = attacher(pending);

        assertThat(transactionService.findPendingProviderWithdrawals())
                .extracting(Transaction::getId)
                .contains(pending.getId());

        transactionService.settleMobileMoneyWithdrawal(payoutId, true);

        assertThat(transactionService.findPendingProviderWithdrawals())
                .as("une fois soldé, un retrait n'a plus à être relu chez le prestataire")
                .extracting(Transaction::getId)
                .doesNotContain(pending.getId());
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private Transaction ouvrirRetrait(String montant) {
        return ouvrirRetrait(montant, UUID.randomUUID().toString());
    }

    private Transaction ouvrirRetrait(String montant, String cle) {
        return transactionService.openMobileMoneyWithdrawal(client, new MobileMoneyWithdrawalRequest(
                wallet.getId(),
                new BigDecimal(montant),
                MobileMoneyMode.MOOV_TOGO,
                "+22890000002",
                cle
        ));
    }

    /** Simule la prise en charge par le prestataire, sans l'appeler. */
    private String attacher(Transaction pending) {
        String payoutId = "payout_" + UUID.randomUUID();
        transactionService.attachProviderTransaction(pending.getId(), "FEDAPAY", payoutId, null);
        return payoutId;
    }

    private TransactionStatus statutDe(Transaction tx) {
        return transactionRepository.findById(tx.getId()).orElseThrow().getStatus();
    }

    private User givenUser() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        User user = userRepository.save(User.builder()
                .firstName("Momo")
                .lastName("Retrait")
                .email("retrait-" + unique + "@kola.test")
                .phoneNumber("+22895" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
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

    private Wallet givenWallet(User owner, String balance) {
        return walletRepository.save(Wallet.builder()
                .currency("XOF")
                .balance(new BigDecimal(balance))
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(owner)
                .build());
    }

    private Wallet reload() {
        return walletRepository.findById(wallet.getId()).orElseThrow();
    }
}
