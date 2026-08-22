package com.kola.backend.scheduler;

import com.kola.backend.role.Role;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.vault.Vault;
import com.kola.backend.vault.VaultRepository;
import com.kola.backend.vault.VaultStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration du batch des virements programmés.
 *
 * Comme pour le verrouillage de compte, un test Mockito serait passé au vert
 * sur le bug qu'il couvre : les mocks rendent invisible le fait que les
 * entités renvoyées hors transaction sont détachées, et donc que
 * `wallet.setBalance(...)` ne produit aucun UPDATE. Il faut une vraie base et
 * une vraie transaction — d'où @SpringBootTest sans @Transactional sur la
 * classe (une transaction de test engloberait celle du batch et masquerait à
 * nouveau le problème).
 */
@SpringBootTest
class ScheduledTransferExecutionTest {

    @Autowired
    private ScheduledTransferService scheduledTransferService;
    @Autowired
    private ScheduledTransferRepository scheduledRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private VaultRepository vaultRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent : DataInitializer n'a pas tourné"));

        owner = userRepository.save(User.builder()
                .firstName("Sched")
                .lastName("Uler")
                .email("sched-" + unique + "@kola.test")
                .phoneNumber("+229" + String.format("%08d", Math.abs(unique.hashCode()) % 100_000_000))
                .countryCode("BJ")
                .password(passwordEncoder.encode("Test1234"))
                .roles(List.of(clientRole))
                .enabled(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build());
    }

    @AfterEach
    void tearDown() {
        List<Wallet> wallets = walletRepository.findByOwnerId(owner.getId());
        scheduledRepository.deleteAll(scheduledRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()));
        wallets.forEach(w -> transactionRepository.deleteAll(
                transactionRepository.findByWalletIdOrderByCreatedAtDesc(w.getId())));
        vaultRepository.deleteAll(vaultRepository.findByOwnerId(owner.getId()));
        walletRepository.deleteAll(wallets);
        userRepository.delete(owner);
    }

    @Test
    void leVirementSansCoffreDebiteReellementLeWallet() {
        Wallet wallet = givenWallet("10000");
        ScheduledTransfer st = givenDueTransfer(wallet, null, "3000");

        scheduledTransferService.processScheduledTransfers();

        assertThat(reload(wallet).getBalance())
                .as("le solde doit réellement être débité, pas seulement tracé")
                .isEqualByComparingTo("7000");

        Transaction tx = onlyTransactionOf(wallet);
        assertThat(tx.getType()).isEqualTo(TransactionType.SCHEDULED_TRANSFER);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getAmount()).isEqualByComparingTo("3000");

        ScheduledTransfer after = scheduledRepository.findById(st.getId()).orElseThrow();
        assertThat(after.getLastExecutedAt()).isNotNull();
        assertThat(after.getNextExecutionDate())
                .as("la prochaine échéance doit être replanifiée dans le futur")
                .isAfter(LocalDateTime.now());
    }

    @Test
    void leVirementVersUnCoffreImmobiliseLesFondsEtAlimenteLeCoffre() {
        Wallet wallet = givenWallet("10000");
        Vault vault = givenVault(wallet);
        givenDueTransfer(wallet, vault, "2500");

        scheduledTransferService.processScheduledTransfers();

        Wallet after = reload(wallet);
        assertThat(after.getBalance())
                .as("alimenter un coffre immobilise, ne fait pas sortir l'argent")
                .isEqualByComparingTo("10000");
        assertThat(after.getLockedBalance()).isEqualByComparingTo("2500");
        assertThat(vaultRepository.findById(vault.getId()).orElseThrow().getCurrentAmount())
                .isEqualByComparingTo("2500");
    }

    @Test
    void leSoldeInsuffisantTraceUnEchecSansDebiterNiBoucler() {
        Wallet wallet = givenWallet("1000");
        ScheduledTransfer st = givenDueTransfer(wallet, null, "5000");

        scheduledTransferService.processScheduledTransfers();

        assertThat(reload(wallet).getBalance()).isEqualByComparingTo("1000");

        Transaction tx = onlyTransactionOf(wallet);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);

        assertThat(scheduledRepository.findById(st.getId()).orElseThrow().getNextExecutionDate())
                .as("replanifié malgré l'échec, sinon le batch retenterait en boucle")
                .isAfter(LocalDateTime.now());
    }

    @Test
    void lEchecDUnVirementNEmpechePasLesAutresDeSExecuter() {
        Wallet pauvre = givenWallet("100");
        Wallet garni = givenWallet("10000");
        givenDueTransfer(pauvre, null, "5000");   // échouera
        givenDueTransfer(garni, null, "4000");    // doit passer quand même

        scheduledTransferService.processScheduledTransfers();

        assertThat(reload(pauvre).getBalance()).isEqualByComparingTo("100");
        assertThat(reload(garni).getBalance())
                .as("chaque virement a sa propre transaction : un échec n'annule pas les autres")
                .isEqualByComparingTo("6000");
    }

    // ── Fixtures ───────────────────────────────────────────────────

    private Wallet givenWallet(String balance) {
        // Une devise distincte par wallet : existsByOwnerIdAndCurrency impose
        // l'unicité par couple (propriétaire, devise).
        String currency = walletRepository.findByOwnerId(owner.getId()).isEmpty() ? "XOF" : "EUR";
        return walletRepository.save(Wallet.builder()
                .currency(currency)
                .balance(new BigDecimal(balance))
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(owner)
                .build());
    }

    private Vault givenVault(Wallet wallet) {
        return vaultRepository.save(Vault.builder()
                .name("Coffre test")
                .currentAmount(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .unlockDate(LocalDate.now().plusMonths(6))
                .status(VaultStatus.ACTIVE)
                .owner(owner)
                .wallet(wallet)
                .build());
    }

    private ScheduledTransfer givenDueTransfer(Wallet wallet, Vault targetVault, String amount) {
        return scheduledRepository.save(ScheduledTransfer.builder()
                .frequency(ScheduledTransfer.Frequency.MONTHLY)
                .executionDay(15)
                .amount(new BigDecimal(amount))
                .currency(wallet.getCurrency())
                .description("Epargne automatique")
                .status(ScheduledTransfer.ScheduledStatus.ACTIVE)
                .owner(owner)
                .wallet(wallet)
                .targetVault(targetVault)
                // Échéance dans le passé → le batch doit la ramasser
                .nextExecutionDate(LocalDateTime.now().minusDays(1))
                .build());
    }

    private Wallet reload(Wallet wallet) {
        return walletRepository.findById(wallet.getId()).orElseThrow();
    }

    private Transaction onlyTransactionOf(Wallet wallet) {
        List<Transaction> txs = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
        assertThat(txs).hasSize(1);
        return txs.get(0);
    }
}
