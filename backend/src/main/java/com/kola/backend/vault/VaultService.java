package com.kola.backend.vault;

import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.VaultLockedException;
import com.kola.backend.transaction.Transaction;
import com.kola.backend.transaction.TransactionRepository;
import com.kola.backend.transaction.TransactionReferenceGenerator;
import com.kola.backend.transaction.TransactionStatus;
import com.kola.backend.transaction.TransactionType;
import com.kola.backend.user.User;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final VaultRepository vaultRepository;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    // ═══════════════════════════════════════════════════════════════
    //  CRÉATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public VaultResponse createVault(User currentUser, CreateVaultRequest request) {
        // 1. Verrouiller le wallet EN PREMIER pour éviter les deadlocks
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, request.walletId());

        BigDecimal initialAmount = request.initialAmount();
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());

        if (available.compareTo(initialAmount) < 0) {
            throw new InsufficientFundsException(
                    "Solde disponible insuffisant pour créer ce coffre : " + available + " " + wallet.getCurrency()
            );
        }

        // CORRECTION FINANCIÈRE : L'argent quitte le solde disponible et va dans le coffre.
        wallet.setBalance(wallet.getBalance().subtract(initialAmount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(initialAmount));

        Vault vault = Vault.builder()
                .name(request.name())
                .purpose(request.purpose())
                .targetAmount(request.targetAmount())
                .currentAmount(initialAmount)
                .currency(wallet.getCurrency())
                .unlockDate(request.unlockDate())
                .status(VaultStatus.ACTIVE)
                .owner(currentUser)
                .wallet(wallet)
                .build();

        vault = vaultRepository.save(vault);

        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            recordVaultTransaction(currentUser, wallet, vault, TransactionType.VAULT_LOCK, initialAmount,
                    "Création coffre '" + vault.getName() + "'");
        }

        return VaultResponse.fromEntity(vault);
    }

    // ═══════════════════════════════════════════════════════════════
    //  AJOUT DE FONDS À UN COFFRE ACTIF
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public VaultResponse addFunds(User currentUser, Long vaultId, AddFundsRequest request) {
        // 1. Récupérer le vault (non verrouillé, juste pour les contrôles)
        Vault vault = findOwnedVaultOrThrow(currentUser, vaultId);
        refreshStatusIfDue(vault);

        if (vault.getStatus() != VaultStatus.ACTIVE) {
            throw new VaultLockedException(
                    "Impossible d'ajouter des fonds : ce coffre n'est plus actif (statut : " + vault.getStatus() + ")"
            );
        }

        // 2. Verrouiller le wallet EN SECOND (ordre cohérent : Wallet puis Vault)
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, vault.getWallet().getId());

        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Solde disponible insuffisant : " + available + " " + wallet.getCurrency()
            );
        }

        // CORRECTION FINANCIÈRE
        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        wallet.setLockedBalance(wallet.getLockedBalance().add(request.amount()));

        vault.setCurrentAmount(vault.getCurrentAmount().add(request.amount()));

        recordVaultTransaction(currentUser, wallet, vault, TransactionType.VAULT_LOCK, request.amount(),
                "Ajout de fonds au coffre '" + vault.getName() + "'");

        return VaultResponse.fromEntity(vault);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DÉBLOCAGE (à l'échéance ou manuel après échéance)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public VaultResponse unlock(User currentUser, Long vaultId) {
        Vault vault = findOwnedVaultOrThrow(currentUser, vaultId);
        refreshStatusIfDue(vault);

        if (vault.getStatus() == VaultStatus.ACTIVE) {
            throw new VaultLockedException(
                    "Ce coffre est verrouillé jusqu'au " + vault.getUnlockDate()
                            + ". Utilisez /close pour une fermeture anticipée (si autorisée)."
            );
        }
        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new VaultLockedException("Ce coffre a déjà été fermé.");
        }

        releaseFundsToWallet(currentUser, vault, "Déblocage coffre '" + vault.getName() + "' à échéance");
        return VaultResponse.fromEntity(vault);
    }

    @Transactional
    public VaultResponse closeEarly(User currentUser, Long vaultId) {
        Vault vault = findOwnedVaultOrThrow(currentUser, vaultId);
        refreshStatusIfDue(vault);

        if (vault.getStatus() == VaultStatus.CLOSED) {
            throw new VaultLockedException("Ce coffre a déjà été fermé.");
        }
        if (vault.getStatus() == VaultStatus.UNLOCKED) {
            vault.setStatus(VaultStatus.CLOSED);
            return VaultResponse.fromEntity(vault);
        }

        releaseFundsToWallet(currentUser, vault, "Fermeture anticipée coffre '" + vault.getName() + "'");
        vault.setStatus(VaultStatus.CLOSED);
        return VaultResponse.fromEntity(vault);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true) // Optimisation : pas besoin d'une transaction ouverte pour lire
    public List<VaultResponse> getMyVaults(User currentUser) {
        List<Vault> vaults = vaultRepository.findByOwnerId(currentUser.getId());
        vaults.forEach(this::refreshStatusIfDue);
        return vaults.stream().map(VaultResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public VaultResponse getVaultById(User currentUser, Long vaultId) {
        Vault vault = findOwnedVaultOrThrow(currentUser, vaultId);
        refreshStatusIfDue(vault);
        return VaultResponse.fromEntity(vault);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════════════════

    private Vault findOwnedVaultOrThrow(User currentUser, Long vaultId) {
        Vault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new EntityNotFoundException("Coffre-fort introuvable"));

        if (!vault.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce coffre-fort ne vous appartient pas");
        }
        return vault;
    }

    private void refreshStatusIfDue(Vault vault) {
        if (vault.getStatus() == VaultStatus.ACTIVE
                && vault.getUnlockDate() != null
                && !vault.getUnlockDate().isAfter(LocalDate.now())) {
            vault.setStatus(VaultStatus.UNLOCKED);
        }
    }

    private void releaseFundsToWallet(User currentUser, Vault vault, String description) {
        // 1. Verrouiller le wallet EN PREMIER
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, vault.getWallet().getId());

        BigDecimal amountToRelease = vault.getCurrentAmount();

        // CORRECTION FINANCIÈRE : L'argent revient dans le solde disponible.
        wallet.setBalance(wallet.getBalance().add(amountToRelease));
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amountToRelease));

        recordVaultTransaction(currentUser, wallet, vault, TransactionType.VAULT_UNLOCK, amountToRelease, description);

        vault.setCurrentAmount(BigDecimal.ZERO);
        if (vault.getStatus() == VaultStatus.ACTIVE) {
            vault.setStatus(VaultStatus.UNLOCKED);
        }
    }

    private void recordVaultTransaction(User currentUser, Wallet wallet, Vault vault,
                                        TransactionType type, BigDecimal amount, String description) {
        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(currentUser)
                .description(description)
                .build();
        transactionRepository.save(tx);
    }
}