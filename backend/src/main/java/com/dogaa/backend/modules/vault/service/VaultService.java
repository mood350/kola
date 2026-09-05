package com.dogaa.backend.modules.vault.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.InsufficientFundsException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.vault.dto.CreateVaultRequest;
import com.dogaa.backend.modules.vault.dto.UpdateVaultRequest;
import java.time.LocalDate;
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

    /**
     * Renames a goal or moves its target.
     *
     * <p>A savings goal outlives the intention that created it: rent becomes a deposit, a wedding
     * moves, an amount turns out to be wrong. Forcing someone to close the vault and open another
     * would mean withdrawing the money and paying it back in, which loses the history the score is
     * computed from — the user would be punished for changing their mind.
     *
     * <p>What cannot be edited: the balance, which moves only through a deposit, a withdrawal or a
     * scheduled payment, each leaving a transaction behind; and the currency, fixed by the wallet
     * the money is locked against.
     */
    @Transactional
    public Vault updateVault(UUID ownerId, UUID vaultId, UpdateVaultRequest request) {
        Vault vault = getVault(ownerId, vaultId);
        requireActive(vault);

        if (request.name() != null && !request.name().isBlank()) {
            vault.setName(request.name().trim());
        }
        if (request.description() != null) {
            vault.setDescription(request.description().isBlank() ? null : request.description());
        }

        if (request.clearTargetAmount()) {
            vault.setTargetAmount(null);
        } else if (request.targetAmount() != null) {
            // Below what is already saved the goal would show as met the moment it is set, which
            // reads as a bug rather than as an achievement.
            if (request.targetAmount().compareTo(vault.getBalance()) < 0) {
                throw new BadRequestException("L'objectif ne peut pas être inférieur aux "
                        + vault.getBalance() + " " + vault.getCurrency() + " déjà épargnés");
            }
            vault.setTargetAmount(request.targetAmount());
        }

        if (request.clearTargetDate()) {
            vault.setTargetDate(null);
        } else if (request.targetDate() != null) {
            if (request.targetDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("L'échéance ne peut pas être dans le passé");
            }
            vault.setTargetDate(request.targetDate());
        }
        return vaultRepository.save(vault);
    }

    // --- Money moves --------------------------------------------------

    /**
     * A vault movement seen from both sides: the vault, and the wallet it locks money in.
     *
     * <p>Funding a vault does not change what the user owns — it moves money from the wallet's
     * spendable side to its locked side. A client handed only the vault has no way to refresh the
     * current account without a second round trip, and until it makes that call it keeps showing
     * an available balance the user has just spent. Returning the pair is what
     * {@code POST /wallets/savings/deposit} already does for its two accounts.
     */
    public record VaultMovement(Vault vault, Wallet wallet) {
    }


    @Transactional
    public VaultMovement deposit(UUID ownerId, UUID vaultId, BigDecimal amount) {
        Vault vault = getVault(ownerId, vaultId);
        requireActive(vault);
        Wallet wallet = walletService.lock(vault.getWalletId(), amount);
        vault.setBalance(vault.getBalance().add(amount));
        transactionService.recordVaultMovement(TransactionType.VAULT_DEPOSIT,
                ownerId, vault.getWalletId(), vault.getCurrency(), amount, vault.getName());
        return new VaultMovement(vault, wallet);
    }

    @Transactional
    public VaultMovement withdraw(UUID ownerId, UUID vaultId, BigDecimal amount) {
        Vault vault = getVault(ownerId, vaultId);
        requireActive(vault);
        if (amount.compareTo(vault.getBalance()) > 0) {
            throw new BadRequestException("Amount exceeds the vault balance");
        }
        Wallet wallet = walletService.unlock(vault.getWalletId(), amount);
        vault.setBalance(vault.getBalance().subtract(amount));
        transactionService.recordVaultMovement(TransactionType.VAULT_WITHDRAWAL,
                ownerId, vault.getWalletId(), vault.getCurrency(), amount, vault.getName());
        return new VaultMovement(vault, wallet);
    }

    /**
     * Releases money from a vault so a scheduled payment can spend it.
     *
     * <p>Same movement as {@link #withdraw}, different intent, and worth its own method for two
     * reasons. It takes the fee into account — the caller passes the full amount that will leave
     * the wallet, because a schedule that releases the transfer amount and then takes the
     * commission from the current account is exactly the silent raid on everyday money that vault
     * funding exists to prevent. And it fails with a message naming the vault, since "solde
     * insuffisant" on a scheduled payment is otherwise impossible to act on.
     *
     * <p>Caller-beware: this must run in the same transaction as the payment it funds. If the
     * payment is refused afterwards, the rollback puts the money back under lock; releasing in a
     * separate transaction would leave it loose in the current account.
     */
    @Transactional
    public Vault releaseForPayment(UUID ownerId, UUID vaultId, BigDecimal totalIncludingFee) {
        Vault vault = getVault(ownerId, vaultId);
        requireActive(vault);

        if (totalIncludingFee.compareTo(vault.getBalance()) > 0) {
            throw new InsufficientFundsException(
                    "Le coffre « " + vault.getName() + " » ne contient que " + vault.getBalance()
                            + " " + vault.getCurrency() + " ; " + totalIncludingFee
                            + " sont nécessaires (frais compris).");
        }

        walletService.unlock(vault.getWalletId(), totalIncludingFee);
        vault.setBalance(vault.getBalance().subtract(totalIncludingFee));
        return vault;
    }

    /** Owner closes the vault: everything still locked goes back to the available balance. */
    @Transactional
    public VaultMovement closeVault(UUID ownerId, UUID vaultId) {
        return close(getVault(ownerId, vaultId));
    }

    /**
     * Admin-forced closure (DOGAA.md 4.5). Same effect, no ownership filter.
     *
     * <p>Returns the vault alone: the back-office shows the account, not the client's balances.
     */
    @Transactional
    public Vault forceClose(UUID vaultId) {
        Vault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new ResourceNotFoundException("Vault not found: " + vaultId));
        return close(vault).vault();
    }

    private VaultMovement close(Vault vault) {
        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new BadRequestException("Vault is already closed");
        }
        BigDecimal remaining = vault.getBalance();
        // An empty vault releases nothing, so there is no wallet coming back from an unlock.
        // Read it anyway: the caller gets the same shape whether or not money moved.
        Wallet wallet = walletService.getById(vault.getWalletId());
        if (remaining.signum() > 0) {
            wallet = walletService.unlock(vault.getWalletId(), remaining);
            vault.setBalance(BigDecimal.ZERO);
            transactionService.recordVaultMovement(TransactionType.VAULT_WITHDRAWAL,
                    vault.getOwnerId(), vault.getWalletId(), vault.getCurrency(), remaining, vault.getName());
        }
        vault.setStatus(VaultStatus.CLOSED);
        return new VaultMovement(vault, wallet);
    }

    private void requireActive(Vault vault) {
        if (vault.getStatus() != VaultStatus.ACTIVE) {
            throw new BadRequestException("Vault is closed");
        }
    }
}
