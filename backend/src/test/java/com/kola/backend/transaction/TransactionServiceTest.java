package com.kola.backend.transaction;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.merchant.MerchantRepository;
import com.kola.backend.notification.NotificationService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        when(transactionRepository.sumDepositedTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        DepositRequest request = new DepositRequest(1L, new BigDecimal("500"), "EXT-001", null);
        TransactionResponse response = transactionService.deposit(user, request);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("1500"));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_shouldThrow_whenDailyDepositLimitExceeded() {
        User user = user(1L); // TIER_2 → plafond de rechargement 2 000 000 XOF/jour
        Wallet wallet = wallet(1L, new BigDecimal("1000"), BigDecimal.ZERO, user);
        when(walletService.findOwnedWalletForUpdateOrThrow(user, 1L)).thenReturn(wallet);
        when(transactionRepository.sumDepositedTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1900000"));

        DepositRequest request = new DepositRequest(1L, new BigDecimal("200000"), null, null);

        assertThatThrownBy(() -> transactionService.deposit(user, request))
                .isInstanceOf(KycLimitExceededException.class);

        assertThat(wallet.getBalance())
                .as("aucun crédit ne doit avoir lieu quand le plafond est dépassé")
                .isEqualByComparingTo(new BigDecimal("1000"));
        verify(transactionRepository, never()).save(any(Transaction.class));
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

        WithdrawalRequest request = new WithdrawalRequest(1L, new BigDecimal("10000"), null);
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

        WithdrawalRequest request = new WithdrawalRequest(1L, new BigDecimal("10000"), null);

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

        WithdrawalRequest request = new WithdrawalRequest(1L, new BigDecimal("100000"), null);

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

        // Bénéficiaire externe : aucun compte Kola derrière ce numéro.
        when(userRepository.findByPhoneNumber("+22890000001")).thenReturn(Optional.empty());
        when(walletService.findOwnedActiveWalletOrThrow(user, 1L)).thenReturn(wallet);
        when(walletService.lockAllForUpdate(List.of(1L))).thenReturn(Map.of(1L, wallet));
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000003", "KLA-2026-00000004");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(transactionRepository.sumSpentTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        TransferRequest request = new TransferRequest(1L, 10L, new BigDecimal("20000"), "Test transfer", null);
        TransactionResponse response = transactionService.transfer(user, request);

        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("20000"));
        // TRANSFER_OUT + FEE, pas de TRANSFER_IN : le destinataire est externe.
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_shouldCreditRecipient_whenBeneficiaryIsKolaUser() {
        User sender = user(1L);
        User recipient = user(2L);
        recipient.setPhoneNumber("+22890000009");
        Wallet senderWallet = wallet(1L, new BigDecimal("100000"), BigDecimal.ZERO, sender);
        Wallet recipientWallet = wallet(2L, new BigDecimal("500"), BigDecimal.ZERO, recipient);
        Beneficiary beneficiary = Beneficiary.builder()
                .id(10L)
                .alias("Ami Kola")
                .phoneNumber("+22890000009")
                .countryCode("TG")
                .owner(sender)
                .build();

        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(beneficiary));
        when(userRepository.findByPhoneNumber("+22890000009")).thenReturn(Optional.of(recipient));
        when(walletService.findOwnedActiveWalletOrThrow(sender, 1L)).thenReturn(senderWallet);
        when(walletService.findOrCreateReceivingWallet(recipient, "XOF")).thenReturn(recipientWallet);
        when(walletService.lockAllForUpdate(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, senderWallet, 2L, recipientWallet));
        when(referenceGenerator.generate()).thenReturn("KLA-2026-00000005", "KLA-2026-00000006", "KLA-2026-00000007");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.sumSpentTodayBySender(eq(1L), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        TransferRequest request = new TransferRequest(1L, 10L, new BigDecimal("20000"), null, null);
        transactionService.transfer(sender, request);

        BigDecimal fee = new BigDecimal("20000").multiply(new BigDecimal("0.015"));
        assertThat(senderWallet.getBalance())
                .isEqualByComparingTo(new BigDecimal("100000").subtract(new BigDecimal("20000")).subtract(fee));
        assertThat(recipientWallet.getBalance())
                .as("le destinataire reçoit le montant net, les frais restent à la charge de l'émetteur")
                .isEqualByComparingTo(new BigDecimal("20500"));

        // TRANSFER_OUT + FEE + TRANSFER_IN
        verify(transactionRepository, times(3)).save(any(Transaction.class));
        verify(notificationService).notify(eq(recipient), eq("Transfert reçu"), any(), any());
    }

    @Test
    void transfer_shouldThrow_whenBeneficiaryIsSelf() {
        User user = user(1L);
        user.setPhoneNumber("+22890000003");
        Beneficiary self = Beneficiary.builder()
                .id(10L)
                .alias("Moi-même")
                .phoneNumber("+22890000003")
                .countryCode("TG")
                .owner(user)
                .build();

        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(self));
        when(userRepository.findByPhoneNumber("+22890000003")).thenReturn(Optional.of(user));

        TransferRequest request = new TransferRequest(1L, 10L, new BigDecimal("5000"), null, null);

        assertThatThrownBy(() -> transactionService.transfer(user, request))
                .isInstanceOf(IllegalArgumentException.class);
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

        // Plus aucun stub de wallet : l'appartenance du bénéficiaire est
        // désormais vérifiée AVANT de toucher au moindre portefeuille.
        when(beneficiaryRepository.findById(10L)).thenReturn(Optional.of(otherBeneficiary));

        TransferRequest request = new TransferRequest(1L, 10L, new BigDecimal("5000"), null, null);

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
