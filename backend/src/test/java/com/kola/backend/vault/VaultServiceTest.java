package com.kola.backend.vault;

import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verrouille l'invariant comptable des coffres :
 * `balance` est le solde TOTAL, `lockedBalance` la part immobilisée à
 * l'intérieur de ce total, et disponible = balance - lockedBalance.
 * Bloquer/débloquer ne doit donc JAMAIS toucher `balance` — sinon le montant
 * est compté deux fois (disponible amputé du double) et `lockedBalance`
 * peut devenir négatif au déblocage.
 */
@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultRepository vaultRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    @InjectMocks
    private VaultService vaultService;

    private User user() {
        return User.builder().id(1L).kycLevel(KycLevel.TIER_2).phoneNumber("+22890000000").build();
    }

    private Wallet wallet(BigDecimal balance, BigDecimal locked, User owner) {
        return Wallet.builder()
                .id(1L)
                .currency("XOF")
                .balance(balance)
                .lockedBalance(locked)
                .active(true)
                .owner(owner)
                .build();
    }

    private BigDecimal available(Wallet w) {
        return w.getBalance().subtract(w.getLockedBalance());
    }

    @Test
    void createVault_shouldLockFundsWithoutReducingTotalBalance() {
        User user = user();
        Wallet wallet = wallet(new BigDecimal("10000"), BigDecimal.ZERO, user);

        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000001");
        when(vaultRepository.save(any(Vault.class))).thenAnswer(inv -> inv.getArgument(0));

        vaultService.createVault(user, new CreateVaultRequest(
                1L, "Vacances", null, null, new BigDecimal("3000"), LocalDate.now().plusMonths(6)));

        assertThat(wallet.getBalance()).isEqualByComparingTo("10000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("3000");
        // Le disponible ne baisse que du montant bloqué, pas du double.
        assertThat(available(wallet)).isEqualByComparingTo("7000");
    }

    @Test
    void unlock_shouldRestoreAvailableBalanceExactly() {
        User user = user();
        Wallet wallet = wallet(new BigDecimal("10000"), new BigDecimal("3000"), user);
        Vault vault = Vault.builder()
                .id(5L)
                .name("Vacances")
                .currentAmount(new BigDecimal("3000"))
                .currency("XOF")
                .unlockDate(LocalDate.now().minusDays(1)) // échéance atteinte
                .status(VaultStatus.ACTIVE)
                .owner(user)
                .wallet(wallet)
                .build();

        when(vaultRepository.findById(5L)).thenReturn(Optional.of(vault));
        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000002");

        vaultService.unlock(user, 5L);

        assertThat(wallet.getBalance()).isEqualByComparingTo("10000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(available(wallet)).isEqualByComparingTo("10000");
        assertThat(vault.getCurrentAmount()).isEqualByComparingTo("0");
        assertThat(vault.getStatus()).isEqualTo(VaultStatus.UNLOCKED);
    }

    @Test
    void lockThenUnlock_shouldNeverDriveLockedBalanceNegative() {
        User user = user();
        Wallet wallet = wallet(new BigDecimal("5000"), BigDecimal.ZERO, user);

        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000003");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Vault created = Vault.builder()
                .id(7L)
                .name("Test")
                .currentAmount(BigDecimal.ZERO)
                .currency("XOF")
                .status(VaultStatus.ACTIVE)
                .owner(user)
                .wallet(wallet)
                .build();
        when(vaultRepository.findById(7L)).thenReturn(Optional.of(created));

        vaultService.addFunds(user, 7L, new AddFundsRequest(new BigDecimal("2000")));
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("2000");

        // Fermeture anticipée : libère exactement ce qui a été immobilisé.
        vaultService.closeEarly(user, 7L);
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getLockedBalance().signum()).isGreaterThanOrEqualTo(0);
        assertThat(available(wallet)).isEqualByComparingTo("5000");
    }
}
