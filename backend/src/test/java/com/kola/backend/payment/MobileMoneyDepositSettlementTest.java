package com.kola.backend.payment;

import com.kola.backend.notification.NotificationRepository;
import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.transaction.MobileMoneyDepositRequest;
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
 * Cycle de vie d'un dépôt Mobile Money, de la demande au crédit.
 *
 * CE QUI EST RÉELLEMENT VÉRIFIÉ ICI : qu'un dépôt ne crédite le wallet qu'une
 * seule fois, et seulement quand le prestataire l'a confirmé. C'est la
 * propriété qui, si elle cède, fabrique de l'argent — le webhook de FedaPay est
 * rejoué jusqu'à neuf fois, et une notification interceptée peut être renvoyée.
 *
 * Le prestataire lui-même n'est pas appelé : ces tests portent sur le grand
 * livre, pas sur le transport HTTP.
 */
@SpringBootTest
class MobileMoneyDepositSettlementTest {

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

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(300);
    private final List<User> created = new ArrayList<>();

    private User client;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        client = givenUser();
        wallet = givenWallet(client, "0");
    }

    @AfterEach
    void tearDown() {
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
    void ouvrirUneDemandeNeCrediteRien() {
        Transaction pending = ouvrirDemande("10000");

        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(reload().getBalance())
                .as("tant que l'opérateur n'a pas confirmé, le solde ne bouge pas")
                .isEqualByComparingTo("0");
    }

    @Test
    void laConfirmationDuPrestataireCrediteLeWallet() {
        Transaction pending = ouvrirDemande("10000");
        String providerId = attacher(pending);

        transactionService.settleMobileMoneyDeposit(providerId, true);

        assertThat(reload().getBalance()).isEqualByComparingTo("10000");
        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void unWebhookRejoueNeCrediteQuUneFois() {
        Transaction pending = ouvrirDemande("25000");
        String providerId = attacher(pending);

        // FedaPay relance jusqu'à neuf fois : trois passages suffisent à
        // démontrer que la première seule compte.
        transactionService.settleMobileMoneyDeposit(providerId, true);
        transactionService.settleMobileMoneyDeposit(providerId, true);
        transactionService.settleMobileMoneyDeposit(providerId, true);

        assertThat(reload().getBalance())
                .as("un rejeu de notification ne doit pas fabriquer d'argent")
                .isEqualByComparingTo("25000");
    }

    @Test
    void unRefusDeLOperateurNeCrediteRienEtClasseLEchec() {
        Transaction pending = ouvrirDemande("5000");
        String providerId = attacher(pending);

        transactionService.settleMobileMoneyDeposit(providerId, false);

        assertThat(reload().getBalance()).isEqualByComparingTo("0");
        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void uneConfirmationApresUnRefusNeRattrapePasLEcriture() {
        Transaction pending = ouvrirDemande("5000");
        String providerId = attacher(pending);

        transactionService.settleMobileMoneyDeposit(providerId, false);
        transactionService.settleMobileMoneyDeposit(providerId, true);

        assertThat(reload().getBalance())
                .as("une écriture close ne se rouvre pas : seul l'état PENDING accepte un verdict")
                .isEqualByComparingTo("0");
        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void uneNotificationInconnueEstIgnoreeSansErreur() {
        // Cas réel : le compte marchand est partagé, ou la notification vient
        // d'un autre environnement. Elle doit être acquittée, pas explosée —
        // sinon FedaPay la rejoue puis désactive l'endpoint.
        transactionService.settleMobileMoneyDeposit("operation-qui-nexiste-pas", true);

        assertThat(reload().getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void uneDemandeAbandonneeEstMarqueeEnEchec() {
        Transaction pending = ouvrirDemande("7000");

        transactionService.abandonPendingDeposit(pending.getId());

        assertThat(statutDe(pending)).isEqualTo(TransactionStatus.FAILED);
        assertThat(reload().getBalance()).isEqualByComparingTo("0");
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private Transaction ouvrirDemande(String montant) {
        return transactionService.openMobileMoneyDeposit(client, new MobileMoneyDepositRequest(
                wallet.getId(),
                new BigDecimal(montant),
                MobileMoneyMode.MOOV_TOGO,
                "+22890000001",
                UUID.randomUUID().toString()
        ));
    }

    /** Simule la prise en charge par le prestataire, sans l'appeler. */
    private String attacher(Transaction pending) {
        String providerId = "fedapay-" + UUID.randomUUID();
        transactionService.attachProviderTransaction(pending.getId(), "FEDAPAY", providerId);
        return providerId;
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
                .lastName("Test")
                .email("momo-" + unique + "@kola.test")
                .phoneNumber("+22894" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
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
