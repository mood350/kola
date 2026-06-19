package com.kola.backend.transaction;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    @InjectMocks
    private TransactionService transactionService;

    private User user(Long id) {
        return User.builder()
                .id(id)
                .kycLevel(KycLevel.TIER_2)
                .phoneNumber("+22890000000")
                .build();
    }

    private Wallet wallet(Long id, BigDecimal balance, BigDecimal locked, User owner) {
        return Wallet.builder()
                .id(id)
                .currency("XOF")
                .balance(balance)
                .lockedBalance(locked)
                .active(true)
                .owner(owner)
                .build();
    }

    @Test
    void deposit_shouldIncreaseBalanceAndReturnResponse() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("1000"), BigDecimal.ZERO, user);
        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000001");

        DepositRequest request = new DepositRequest(1L, new BigDecimal("500"), "EXT-001", null);
        TransactionResponse response = transactionService.deposit(user, request);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_shouldDecreaseBalance_whenFundsSufficient() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("50000"), BigDecimal.ZERO, user);
        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000002");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.sumSpentTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        WithdrawalRequest request = new WithdrawalRequest(1L, new BigDecimal("10000"));
        TransactionResponse response = transactionService.withdraw(user, request);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("10000"));
        BigDecimal fee = new BigDecimal("10000").multiply(new BigDecimal("0.01"));
        BigDecimal expectedBalance = new BigDecimal("50000").subtract(new BigDecimal("10000")).subtract(fee);
        assertThat(wallet.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void withdraw_shouldThrow_whenInsufficientFunds() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("100"), BigDecimal.ZERO, user);
        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);

        WithdrawalRequest request = new WithdrawalRequest(1L, new BigDecimal("10000"));

        assertThatThrownBy(() -> transactionService.withdraw(user, request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void withdraw_shouldThrow_whenDailyLimitExceeded() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("5000000"), BigDecimal.ZERO, user);
        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.sumSpentTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("950000"));

        WithdrawalRequest request = new WithdrawalRequest(1L, new BigDecimal("100000"));

        assertThatThrownBy(() -> transactionService.withdraw(user, request))
                .isInstanceOf(KycLimitExceededException.class);
    }

    @Test
    void transfer_shouldSucceed_whenAllChecksPass() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("100000"), BigDecimal.ZERO, user);
        Beneficiary beneficiary = Beneficiary.builder()
                .id(10L)
                .alias("Test Benef")
                .phoneNumber("+22890000001")
                .countryCode("TG")
                .owner(user)
                .build();

        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000003", "KLA-2026-00000004");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.sumSpentTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        TransferRequest request = new TransferRequest(1L, 10L, new BigDecimal("20000"), "Test transfer");
        TransactionResponse response = transactionService.transfer(user, request);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("20000"));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldThrow_whenBeneficiaryNotOwned() {
        User user = user(1L);
        User otherUser = user(2L);
        Wallet wallet = wallet(1L, new BigDecimal("100000"), BigDecimal.ZERO, user);
        Beneficiary otherBeneficiary = Beneficiary.builder()
                .id(10L)
                .alias("Other's Benef")
                .phoneNumber("+22890000002")
                .owner(otherUser)
                .build();

        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(otherBeneficiary));

        TransferRequest request = new TransferRequest(1L, 10L, new BigDecimal("5000"), null);

        assertThatThrownBy(() -> transactionService.transfer(user, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getWalletHistory_shouldReturnPaginatedResults() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("5000"), BigDecimal.ZERO, user);
        Pageable pageable = PageRequest.of(0, 20);

        when(walletService.findOwnedWalletOrThrow(user, 1L)).thenReturn(wallet);
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        Page<TransactionResponse> page = transactionService.getWalletHistory(user, 1L, pageable);

        assertThat(page).isEmpty();
    }

    @Test
    void getByReference_shouldReturnTransaction_whenSender() {
        User user = user(1L);
        Transaction tx = Transaction.builder()
                .reference("KLA-2026-00000001")
                .sender(user)
                .amount(new BigDecimal("5000"))
                .currency("XOF")
                .build();

        when(transactionRepository.findByReference("KLA-2026-00000001")).thenReturn(Optional.of(tx));

        TransactionResponse response = transactionService.getByReference(user, "KLA-2026-00000001");

        assertThat(response.reference()).isEqualTo("KLA-2026-00000001");
    }

    @Test
    void getByReference_shouldThrow_whenNotOwned() {
        User user = user(1L);
        User other = user(2L);
        Transaction tx = Transaction.builder()
                .reference("KLA-2026-00000002")
                .sender(other)
                .receiver(null)
                .build();

        when(transactionRepository.findByReference("KLA-2026-00000002")).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.getByReference(user, "KLA-2026-00000002"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByReference_shouldThrow_whenNotFound() {
        when(transactionRepository.findByReference("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getByReference(user(1L), "INVALID"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deposit_shouldReturnExisting_whenIdempotencyKeyDuplicate() {
        User user = user(1L);
        Wallet wallet = wallet(1L, new BigDecimal("1000"), BigDecimal.ZERO, user);
        Transaction existing = Transaction.builder()
                .id(99L)
                .reference("KLA-2026-00000001")
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(new BigDecimal("500"))
                .currency("XOF")
                .idempotencyKey("idem-001")
                .wallet(wallet)
                .sender(user)
                .build();

        when(transactionRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.of(existing));

        DepositRequest request = new DepositRequest(1L, new BigDecimal("500"), null, "idem-001");
        TransactionResponse response = transactionService.deposit(user, request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.idempotencyKey()).isEqualTo("idem-001");
        verify(transactionRepository, never()).save(any());
    }
}
