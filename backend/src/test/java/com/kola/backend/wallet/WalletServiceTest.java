package com.kola.backend.wallet;

import com.kola.backend.exception.UnsupportedCurrencyException;
import com.kola.backend.exception.WalletInactiveException;
import com.kola.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private User user() {
        return User.builder().id(1L).build();
    }

    private Wallet wallet(Long id, String currency, User owner) {
        return Wallet.builder()
                .id(id)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(owner)
                .build();
    }

    @Test
    void getMyWallets_shouldReturnOwnedWallets() {
        User user = user();
        when(walletRepository.findByOwnerId(1L)).thenReturn(
                List.of(wallet(1L, "XOF", user), wallet(2L, "EUR", user))
        );

        List<WalletResponse> result = walletService.getMyWallets(user);

        assertThat(result).hasSize(2);
        verify(walletRepository).findByOwnerId(1L);
    }

    @Test
    void getWalletById_shouldReturnWallet_whenOwned() {
        User user = user();
        Wallet wallet = wallet(1L, "XOF", user);
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));

        WalletResponse result = walletService.getWalletById(user, 1L);

        assertThat(result.currency()).isEqualTo("XOF");
    }

    @Test
    void getWalletById_shouldThrow_whenNotOwned() {
        User owner = User.builder().id(2L).build();
        User requester = user();
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet(1L, "XOF", owner)));

        assertThatThrownBy(() -> walletService.getWalletById(requester, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getWalletById_shouldThrow_whenNotFound() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWalletById(user(), 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createWallet_shouldCreate_whenCurrencySupported() {
        User user = user();
        when(walletRepository.existsByOwnerIdAndCurrency(1L, "XOF")).thenReturn(false);
        when(walletRepository.save(any())).thenAnswer(i -> i.<Wallet>getArgument(0));

        WalletResponse result = walletService.createWallet(user, new CreateWalletRequest("XOF"));

        assertThat(result.currency()).isEqualTo("XOF");
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createWallet_shouldThrow_whenCurrencyUnsupported() {
        assertThatThrownBy(() -> walletService.createWallet(user(), new CreateWalletRequest("XYZ")))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void createWallet_shouldThrow_whenDuplicateCurrency() {
        User user = user();
        when(walletRepository.existsByOwnerIdAndCurrency(1L, "XOF")).thenReturn(true);

        assertThatThrownBy(() -> walletService.createWallet(user, new CreateWalletRequest("XOF")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findOwnedWalletForUpdateOrThrow_shouldThrow_whenWalletInactive() {
        User user = user();
        Wallet inactive = wallet(1L, "XOF", user);
        inactive.setActive(false);
        when(walletRepository.findOwnedWalletForUpdate(1L, 1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> walletService.findOwnedWalletForUpdateOrThrow(user, 1L))
                .isInstanceOf(WalletInactiveException.class);
    }
}
