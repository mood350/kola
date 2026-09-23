package com.kola.backend.transaction;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.beneficiary.MobileNetwork;
import com.kola.backend.notification.NotificationRepository;
import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.user.KycLevel;
import com.kola.backend.credit.CreditScoreRepository;
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
 * Transfert entre deux comptes Kola, sur base réelle.
 *
 * Le bug couvert est une perte de monnaie : le wallet émetteur était débité et
 * aucune contrepartie n'était jamais créée. Un test à mocks ne pouvait pas le
 * voir — il vérifiait le nombre d'appels à save(), pas la conservation des
 * soldes. D'où @SpringBootTest, sans @Transactional sur la classe pour que les
 * verrous et les commits soient ceux de la production.
 */
@SpringBootTest
class InternalTransferIntegrationTest {

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
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger();

    private final List<User> created = new ArrayList<>();

    private User emetteur;
    private User destinataire;

    @BeforeEach
    void setUp() {
        emetteur = givenUser("emetteur");
        destinataire = givenUser("destinataire");
    }

    @AfterEach
    void tearDown() {
        /* Le score est recalculé après chaque transaction : ces utilisateurs
           ont donc des lignes dans credit_scores, qui référencent _user. Elles
           partent en premier, sinon la suppression viole la clé étrangère. */
        created.forEach(u -> creditScoreRepository.deleteAll(
                creditScoreRepository.findByUserIdOrderByCreatedAtDesc(u.getId())));
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
    void lArgentEnvoyeArriveReellementSurLeWalletDuDestinataire() {
        Wallet walletEmetteur = givenWallet(emetteur, "100000");
        Wallet walletDestinataire = givenWallet(destinataire, "5000");
        Beneficiary beneficiaire = givenBeneficiary(emetteur, destinataire);

        transactionService.transfer(emetteur,
                new TransferRequest(walletEmetteur.getId(), beneficiaire.getId(), new BigDecimal("20000"), null, null));

        BigDecimal frais = new BigDecimal("20000").multiply(new BigDecimal("0.015"));

        assertThat(reload(walletEmetteur).getBalance())
                .isEqualByComparingTo(new BigDecimal("100000").subtract(new BigDecimal("20000")).subtract(frais));

        assertThat(reload(walletDestinataire).getBalance())
                .as("le destinataire doit être réellement crédité")
                .isEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    void laSeuleMonnaieQuiDisparaitEstCelleDesFrais() {
        Wallet walletEmetteur = givenWallet(emetteur, "100000");
        Wallet walletDestinataire = givenWallet(destinataire, "5000");
        Beneficiary beneficiaire = givenBeneficiary(emetteur, destinataire);

        BigDecimal totalAvant = reload(walletEmetteur).getBalance()
                .add(reload(walletDestinataire).getBalance());

        transactionService.transfer(emetteur,
                new TransferRequest(walletEmetteur.getId(), beneficiaire.getId(), new BigDecimal("20000"), null, null));

        BigDecimal totalApres = reload(walletEmetteur).getBalance()
                .add(reload(walletDestinataire).getBalance());
        BigDecimal frais = new BigDecimal("20000").multiply(new BigDecimal("0.015"));

        // Les frais « disparaissent » encore faute de compte de revenus : c'est
        // la lacune de comptabilité en partie double, connue et non traitée
        // ici. Cette assertion la borne explicitement — le jour où un compte de
        // contrepartie existera, elle devra devenir une égalité stricte.
        assertThat(totalAvant.subtract(totalApres))
                .as("hors frais, aucune monnaie ne doit être détruite par un transfert interne")
                .isEqualByComparingTo(frais);
    }

    @Test
    void leDestinataireSansWalletDansLaDeviseEnvoyeeSeVoitEnCreerUn() {
        Wallet walletEmetteur = givenWallet(emetteur, "50000");
        Beneficiary beneficiaire = givenBeneficiary(emetteur, destinataire);

        assertThat(walletRepository.findByOwnerId(destinataire.getId())).isEmpty();

        transactionService.transfer(emetteur,
                new TransferRequest(walletEmetteur.getId(), beneficiaire.getId(), new BigDecimal("10000"), null, null));

        List<Wallet> walletsDestinataire = walletRepository.findByOwnerId(destinataire.getId());
        assertThat(walletsDestinataire).hasSize(1);
        assertThat(walletsDestinataire.get(0).getCurrency()).isEqualTo("XOF");
        assertThat(walletsDestinataire.get(0).getBalance()).isEqualByComparingTo("10000");
    }

    @Test
    void leDestinataireVoitLaTransactionDansSonHistoriqueEtEstNotifie() {
        Wallet walletEmetteur = givenWallet(emetteur, "50000");
        Wallet walletDestinataire = givenWallet(destinataire, "0");
        Beneficiary beneficiaire = givenBeneficiary(emetteur, destinataire);

        transactionService.transfer(emetteur,
                new TransferRequest(walletEmetteur.getId(), beneficiaire.getId(), new BigDecimal("10000"), null, null));

        List<Transaction> historique =
                transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletDestinataire.getId());

        assertThat(historique).hasSize(1);
        Transaction recu = historique.get(0);
        assertThat(recu.getType()).isEqualTo(TransactionType.TRANSFER_IN);
        assertThat(recu.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(recu.getAmount()).isEqualByComparingTo("10000");
        assertThat(recu.getReceiver().getId())
                .as("`receiver` était structurellement null : getByReference ne pouvait jamais l'autoriser")
                .isEqualTo(destinataire.getId());

        assertThat(notificationRepository.countByUserIdAndReadFalse(destinataire.getId()))
                .isEqualTo(1);
    }

    @Test
    void unBeneficiaireExterneNeCreeAucuneContrepartie() {
        Wallet walletEmetteur = givenWallet(emetteur, "50000");
        // Numéro qui ne correspond à aucun compte Kola
        Beneficiary externe = beneficiaryRepository.save(Beneficiary.builder()
                .alias("Contact externe")
                .phoneNumber("+22899" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
                .countryCode("TG")
                .network(MobileNetwork.MOOV_TOGO)
                .owner(emetteur)
                .build());

        TransactionResponse response = transactionService.transfer(emetteur,
                new TransferRequest(walletEmetteur.getId(), externe.getId(), new BigDecimal("10000"), null, null));

        assertThat(response.type()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(transactionRepository.findByReference(response.reference()).orElseThrow().getReceiver())
                .as("aucun destinataire interne : le règlement reste à la charge de l'opérateur Mobile Money")
                .isNull();
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private User givenUser(String prefix) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        User user = userRepository.save(User.builder()
                .firstName(prefix)
                .lastName("Test")
                .email(prefix + "-" + unique + "@kola.test")
                .phoneNumber("+22897" + String.format("%06d", PHONE_SEQ.incrementAndGet()))
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

    private Beneficiary givenBeneficiary(User owner, User target) {
        return beneficiaryRepository.save(Beneficiary.builder()
                .alias(target.getFirstName())
                .phoneNumber(target.getPhoneNumber())
                .countryCode("TG")
                .network(MobileNetwork.MOOV_TOGO)
                .owner(owner)
                .build());
    }

    private Wallet reload(Wallet wallet) {
        return walletRepository.findById(wallet.getId()).orElseThrow();
    }
}
