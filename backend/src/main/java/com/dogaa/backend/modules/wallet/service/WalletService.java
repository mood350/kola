package com.dogaa.backend.modules.wallet.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.InsufficientFundsException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.entity.WalletStatus;
import com.dogaa.backend.modules.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Owns the {@link Wallet} aggregate and every balance movement (DOGAA.md 4.1).
 *
 * <p>The transaction, vault, credit and scheduling modules move money only through
 * {@link #credit}, {@link #debit}, {@link #lock} and {@link #unlock} — they never touch
 * {@link WalletRepository} or mutate a balance themselves. Each of those methods re-reads
 * the wallet under a {@code PESSIMISTIC_WRITE} lock, so a movement always sees the balance
 * left by the previous one.
 *
 * <p>Balances are never negative: any move that would break that is rejected with
 * {@link InsufficientFundsException} and the caller's transaction rolls back.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;

    // --- Wallet lifecycle ---------------------------------------------------

    @Transactional
    public Wallet createWallet(UUID ownerId, Currency currency) {
        userService.getById(ownerId); // 404s if the owner does not exist
        if (walletRepository.existsByOwnerIdAndCurrency(ownerId, currency)) {
            throw new ConflictException("A " + currency + " wallet already exists for this user");
        }
        Wallet wallet = Wallet.builder()
                .ownerId(ownerId)
                .currency(currency)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public List<Wallet> listWallets(UUID ownerId) {
        return walletRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID ownerId, Currency currency) {
        return walletRepository.findByOwnerIdAndCurrency(ownerId, currency)
                .orElseThrow(() -> new ResourceNotFoundException("No " + currency + " wallet for this user"));
    }

    @Transactional(readOnly = true)
    public Wallet getById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));
    }

    // --- Public operations ------------------------------------------------

    /** Cash-in (DOGAA.md 5.3.A) — free, credits the available balance. */
    @Transactional
    public Wallet deposit(UUID ownerId, Currency currency, BigDecimal amount) {
        Wallet wallet = getWallet(ownerId, currency);
        return credit(wallet.getId(), amount);
    }

    // --- Balance movements (called by the other modules) -----------------

    /** Incoming money: cash-in, received transfer, loan disbursement. Allowed while frozen. */
    @Transactional
    public Wallet credit(UUID walletId, BigDecimal amount) {
        Wallet wallet = lockForUpdate(walletId);
        requirePositive(amount);
        requireNotClosed(wallet);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        return wallet;
    }

    /** Outgoing money: sent transfer, merchant payment, cash-out, fee, loan repayment. */
    @Transactional
    public Wallet debit(UUID walletId, BigDecimal amount) {
        Wallet wallet = lockForUpdate(walletId);
        requirePositive(amount);
        requireActive(wallet);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Available balance is too low for this operation");
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        return wallet;
    }

    /** Vault deposit (DOGAA.md 4.2): move money out of the spendable balance into the locked balance. */
    @Transactional
    public Wallet lock(UUID walletId, BigDecimal amount) {
        Wallet wallet = lockForUpdate(walletId);
        requirePositive(amount);
        requireActive(wallet);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Available balance is too low to lock this amount");
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        return wallet;
    }

    /** Vault withdrawal or forced closure (DOGAA.md 4.5): release locked money back to spendable. */
    @Transactional
    public Wallet unlock(UUID walletId, BigDecimal amount) {
        Wallet wallet = lockForUpdate(walletId);
        requirePositive(amount);
        requireNotClosed(wallet);
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Locked balance is too low to release this amount");
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        return wallet;
    }

    // --- Guards ---------------------------------------------------------

    private Wallet lockForUpdate(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }

    private void requireActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException(
                    "Wallet is " + wallet.getStatus().name().toLowerCase() + "; debits are blocked");
        }
    }

    private void requireNotClosed(Wallet wallet) {
        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new BadRequestException("Wallet is closed");
        }
    }
}
