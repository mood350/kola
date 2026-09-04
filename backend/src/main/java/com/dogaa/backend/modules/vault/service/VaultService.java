package com.dogaa.backend.modules.vault.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.vault.dto.CreateVaultRequest;
import com.dogaa.backend.modules.vault.entity.Vault;
import com.dogaa.backend.modules.vault.entity.VaultStatus;
import com.dogaa.backend.modules.vault.repository.VaultRepository;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Owns the {@link Vault} aggregate (DOGAA.md 4.2). A deposit locks money in the funding
 * wallet ({@link WalletService#lock}); a withdrawal or a close releases it
 * ({@link WalletService#unlock}). Every move also writes a transaction trace through
 * {@link TransactionService} so vault activity appears in the user's history.
 *
 * <p>Each operation is one transaction: the wallet move, the updated vault balance and the
 * trace commit together or not at all.
 */
@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultRepository vaultRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final TransactionService transactionService;

    // --- Lifecycle -----------------------------------------------------

    @Transactional
    public Vault createVault(UUID ownerId, CreateVaultRequest request) {
        userService.getById(ownerId); // 404s if the owner does not exist
        Currency currency = request.currency();
        Wallet fundingWallet;
        try {
            fundingWallet = walletService.getWallet(ownerId, currency);
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException("Open a " + currency + " wallet before creating a vault in it");
        }
        Vault vault = Vault.builder()
                .ownerId(ownerId)
                .walletId(fundingWallet.getId())
                .name(request.name().trim())
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .targetAmount(request.targetAmount())
                .targetDate(request.targetDate())
                .description(request.description())
                .status(VaultStatus.ACTIVE)
                .build();
        return vaultRepository.save(vault);
    }

    @Transactional(readOnly = true)
    public List<Vault> listVaults(UUID ownerId) {
        return vaultRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Vault getVault(UUID ownerId, UUID vaultId) {
        return vaultRepository.findByIdAndOwnerId(vaultId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault not found: " + vaultId));
    }

    // --- Money moves --------------------------------------------------

    @Transactional
    public Vault deposit(UUID ownerId, UUID vaultId, BigDecimal amount) {
        Vault vault = getVault(ownerId, vaultId);
        requireActive(vault);
        walletService.lock(vault.getWalletId(), amount);
        vault.setBalance(vault.getBalance().add(amount));
        transactionService.recordVaultMovement(TransactionType.VAULT_DEPOSIT,
                ownerId, vault.getWalletId(), vault.getCurrency(), amount, vault.getName());
        return vault;
    }

    @Transactional
    public Vault withdraw(UUID ownerId, UUID vaultId, BigDecimal amount) {
        Vault vault = getVault(ownerId, vaultId);
        requireActive(vault);
        if (amount.compareTo(vault.getBalance()) > 0) {
            throw new BadRequestException("Amount exceeds the vault balance");
        }
        walletService.unlock(vault.getWalletId(), amount);
        vault.setBalance(vault.getBalance().subtract(amount));
        transactionService.recordVaultMovement(TransactionType.VAULT_WITHDRAWAL,
                ownerId, vault.getWalletId(), vault.getCurrency(), amount, vault.getName());
        return vault;
    }

    /** Owner closes the vault: everything still locked goes back to the available balance. */
    @Transactional
    public Vault closeVault(UUID ownerId, UUID vaultId) {
        return close(getVault(ownerId, vaultId));
    }

    /** Admin-forced closure (DOGAA.md 4.5). Same effect, no ownership filter. */
    @Transactional
    public Vault forceClose(UUID vaultId) {
        Vault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault not found: " + vaultId));
        return close(vault);
    }

    private Vault close(Vault vault) {
        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new BadRequestException("Vault is already closed");
        }
        BigDecimal remaining = vault.getBalance();
        if (remaining.signum() > 0) {
            walletService.unlock(vault.getWalletId(), remaining);
            vault.setBalance(BigDecimal.ZERO);
            transactionService.recordVaultMovement(TransactionType.VAULT_WITHDRAWAL,
                    vault.getOwnerId(), vault.getWalletId(), vault.getCurrency(), remaining, vault.getName());
        }
        vault.setStatus(VaultStatus.CLOSED);
        return vault;
    }

    private void requireActive(Vault vault) {
        if (vault.getStatus() != VaultStatus.ACTIVE) {
            throw new BadRequestException("Vault is closed");
        }
    }
}
