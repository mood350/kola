package com.kola.backend.modules.vault;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.repository.TransactionRepository;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import com.kola.backend.modules.vault.entity.Vault;
import com.kola.backend.modules.vault.entity.VaultStatus;
import com.kola.backend.modules.vault.service.VaultService;
import com.kola.backend.modules.wallet.entity.Wallet;
import com.kola.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the wallet -> vault -> transaction stack end to end: locking money in a vault must
 * move it from the wallet's available balance to its locked balance and leave a trace
 * (KOLA.md 4.2), and closing the vault must give it all back.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaultFlowIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VaultService vaultService;
    @Autowired
    private TransactionRepository transactionRepository;

    private UUID ownerId;

    @BeforeEach
    void createFundedWallet() {
        User user = userService.save(User.builder()
                .firstName("Ama").lastName("Koffi")
                .phone("+22890" + (100000 + (int) (Math.random() * 899999)))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .privacyPolicyAcceptedAt(java.time.Instant.now())
                .build());
        ownerId = user.getId();
        Wallet wallet = walletService.createWallet(ownerId, Currency.XOF);
        walletService.credit(wallet.getId(), new BigDecimal("100000"));
    }

    @Test
    void depositLocksFundsAndCloseReleasesThem() {
        Vault vault = vaultService.createVault(ownerId, new com.kola.backend.modules.vault.dto.CreateVaultRequest(
                "Réparation camion", Currency.XOF, new BigDecimal("80000"),
                LocalDate.now().plusMonths(6), null));

        // The movement hands back both sides, so a client can refresh the vault and the current
        // account from one response — assert on that pair, not only on what is re-read after.
        VaultService.VaultMovement deposited =
                vaultService.deposit(ownerId, vault.getId(), new BigDecimal("30000"));
        assertThat(deposited.vault().getBalance()).isEqualByComparingTo("30000");
        assertThat(deposited.wallet().getAvailableBalance()).isEqualByComparingTo("70000");
        assertThat(deposited.wallet().getLockedBalance()).isEqualByComparingTo("30000");
        assertThat(deposited.wallet().getTotalBalance()).isEqualByComparingTo("100000");

        Wallet wallet = walletService.getWallet(ownerId, Currency.XOF);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("70000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("30000");
        assertThat(wallet.getTotalBalance()).isEqualByComparingTo("100000");
        assertThat(vaultService.getVault(ownerId, vault.getId()).getBalance()).isEqualByComparingTo("30000");

        List<Transaction> traces = transactionRepository.findAll();
        assertThat(traces).anyMatch(t -> t.getType() == TransactionType.VAULT_DEPOSIT
                && t.getAmount().compareTo(new BigDecimal("30000")) == 0
                && t.getFee().signum() == 0);

        VaultService.VaultMovement withdrawn =
                vaultService.withdraw(ownerId, vault.getId(), new BigDecimal("10000"));
        assertThat(withdrawn.wallet().getAvailableBalance()).isEqualByComparingTo("80000");
        assertThat(withdrawn.wallet().getLockedBalance()).isEqualByComparingTo("20000");
        wallet = walletService.getWallet(ownerId, Currency.XOF);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("80000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("20000");

        VaultService.VaultMovement closed = vaultService.closeVault(ownerId, vault.getId());
        assertThat(closed.vault().getStatus()).isEqualTo(VaultStatus.CLOSED);
        assertThat(closed.vault().getBalance()).isEqualByComparingTo("0");
        assertThat(closed.wallet().getAvailableBalance()).isEqualByComparingTo("100000");
        assertThat(closed.wallet().getLockedBalance()).isEqualByComparingTo("0");
        wallet = walletService.getWallet(ownerId, Currency.XOF);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("100000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
    }

    @Test
    void cannotDepositMoreThanTheAvailableBalance() {
        Vault vault = vaultService.createVault(ownerId, new com.kola.backend.modules.vault.dto.CreateVaultRequest(
                "Scolarité", Currency.XOF, null, null, null));

        assertThatThrownBy(() ->
                vaultService.deposit(ownerId, vault.getId(), new BigDecimal("150000")))
                .isInstanceOf(com.kola.backend.exception.InsufficientFundsException.class);
    }

    @Test
    void cannotCreateAVaultWithoutAWalletInThatCurrency() {
        assertThatThrownBy(() ->
                vaultService.createVault(ownerId, new com.kola.backend.modules.vault.dto.CreateVaultRequest(
                        "Trip", Currency.USD, null, null, null)))
                .isInstanceOf(com.kola.backend.exception.BadRequestException.class);
    }
}
