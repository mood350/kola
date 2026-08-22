package com.kola.backend.transaction;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.beneficiary.MobileNetwork;
import com.kola.backend.notification.NotificationRepository;
import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
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
 * Rejeu d'une opération monétaire sous la même clé d'idempotence.
 *
 * Seul le dépôt était protégé ; retrait, transfert et paiement marchand
 * rejouaient intégralement le mouvement d'argent à chaque double-tap ou retry
 * réseau. Ces tests vérifient le seul critère qui compte : le solde ne bouge
 * qu'une fois.
 */
@SpringBootTest
class IdempotencyIntegrationTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private BeneficiaryRepository beneficiaryRepository;
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

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(800);

    private final List<User> created = new ArrayList<>();

    private User payeur;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        payeur = givenUser("payeur");
        wallet = givenWallet(payeur, "100000");
    }

    @AfterEach
    void tearDown() {
        created.forEach(u -> {
            beneficiaryRepository.deleteAll(beneficiaryRepository.findByOwnerId(u.getId()));
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
    void unRetraitRejoueSousLaMemeCleNeDebiteQuUneFois() {
        String cle = UUID.randomUUID().toString();

        TransactionResponse premier = transactionService.withdraw(payeur,
                new WithdrawalRequest(wallet.getId(), new BigDecimal("10000"), cle));
        TransactionResponse rejeu = transactionService.withdraw(payeur,
                new WithdrawalRequest(wallet.getId(), new BigDecimal("10000"), cle));

        assertThat(rejeu.reference())
                .as("le rejeu doit renvoyer la transaction d'origine, pas en créer une seconde")
                .isEqualTo(premier.reference());

        BigDecimal frais = new BigDecimal("10000").multiply(new BigDecimal("0.01"));
        assertThat(reload().getBalance())
                .isEqualByComparingTo(new BigDecimal("100000").subtract(new BigDecimal("10000")).subtract(frais));
    }

    @Test
    void unTransfertRejoueNeCrediteLeDestinataireQuUneFois() {
        User destinataire = givenUser("destinataire");
        Wallet walletDestinataire = givenWallet(destinataire, "0");
        Beneficiary beneficiaire = beneficiaryRepository.save(Beneficiary.builder()
                .alias("Ami")
                .phoneNumber(destinataire.getPhoneNumber())
                .countryCode("TG")
                .network(MobileNetwork.MOOV_TOGO)
                .owner(payeur)
                .build());

        String cle = UUID.randomUUID().toString();
        TransferRequest requete =
                new TransferRequest(wallet.getId(), beneficiaire.getId(), new BigDecimal("20000"), null, cle);

        TransactionResponse premier = transactionService.transfer(payeur, requete);
        TransactionResponse rejeu = transactionService.transfer(payeur, requete);

        assertThat(rejeu.reference()).isEqualTo(premier.reference());
        assertThat(walletRepository.findById(walletDestinataire.getId()).orElseThrow().getBalance())
                .as("un double-tap ne doit pas envoyer l'argent deux fois")
                .isEqualByComparingTo("20000");
    }

    @Test
    void deuxOperationsDistinctesSansCleRestentIndependantes() {
        transactionService.withdraw(payeur, new WithdrawalRequest(wallet.getId(), new BigDecimal("5000"), null));
        transactionService.withdraw(payeur, new WithdrawalRequest(wallet.getId(), new BigDecimal("5000"), null));

        BigDecimal frais = new BigDecimal("5000").multiply(new BigDecimal("0.01"));
        assertThat(reload().getBalance())
                .as("sans clé fournie, chaque appel reste une opération à part entière")
                .isEqualByComparingTo(new BigDecimal("100000")
                        .subtract(new BigDecimal("10000"))
                        .subtract(frais.multiply(new BigDecimal("2"))));
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private User givenUser(String prefix) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        User user = userRepository.save(User.builder()
                .firstName(prefix)
                .lastName("Test")
                .email("idem-" + prefix + "-" + unique + "@kola.test")
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
