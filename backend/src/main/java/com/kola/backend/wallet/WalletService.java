package com.kola.backend.wallet;

import com.kola.backend.exception.UnsupportedCurrencyException;
import com.kola.backend.exception.WalletInactiveException;
import com.kola.backend.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  WalletService.java                         ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * RÈGLES MÉTIER :
 *  - Un utilisateur ne peut avoir qu'un seul wallet par devise.
 *  - Toute opération de débit/crédit doit verrouiller la ligne
 *    (PESSIMISTIC_WRITE) pour éviter les race conditions.
 *  - Le solde disponible = balance - lockedBalance (fonds en coffre-fort).
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    // Devises supportées par Kola, alignées sur les pays couverts par
    // l'enum MobileNetwork (Togo, Sénégal, Côte d'Ivoire, Mali, Burkina Faso,
    // Ghana, Nigeria) + USD/EUR pour les transferts internationaux.
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "XOF", // Togo, Sénégal, Côte d'Ivoire, Mali, Burkina Faso (zone UEMOA)
            "GHS", // Ghana
            "NGN", // Nigeria
            "USD",
            "EUR"
    );

    // ═══════════════════════════════════════════════════════════════
    //  LECTURE
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<WalletResponse> getMyWallets(User currentUser) {
        return walletRepository.findByOwnerId(currentUser.getId())
                .stream()
                .map(WalletResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(User currentUser, Long walletId) {
        Wallet wallet = findOwnedWalletOrThrow(currentUser, walletId);
        return WalletResponse.fromEntity(wallet);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CRÉATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public WalletResponse createWallet(User currentUser, CreateWalletRequest request) {
        String currency = request.currency().toUpperCase();

        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new UnsupportedCurrencyException(
                    "La devise '" + currency + "' n'est pas supportée par Kola."
            );
        }

        if (walletRepository.existsByOwnerIdAndCurrency(currentUser.getId(), currency)) {
            throw new IllegalStateException(
                    "Vous possédez déjà un portefeuille en " + currency
            );
        }

        Wallet wallet = Wallet.builder()
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(currentUser)
                .build();

        wallet = walletRepository.save(wallet);
        return WalletResponse.fromEntity(wallet);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES (utilisés aussi par TransactionService / VaultService)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Récupère un wallet en vérifiant qu'il appartient bien à l'utilisateur
     * courant. Lève AccessDeniedException sinon — étape OBLIGATOIRE pour
     * éviter les failles IDOR (un utilisateur ne doit jamais pouvoir
     * consulter ou manipuler le wallet d'un autre via son ID).
     */
    @Transactional(readOnly = true)
    public Wallet findOwnedWalletOrThrow(User currentUser, Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable"));

        if (!wallet.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce portefeuille ne vous appartient pas");
        }
        return wallet;
    }

    /**
     * Récupère un wallet AVEC verrou pessimiste pour une opération de
     * débit/crédit. À utiliser uniquement à l'intérieur d'une transaction
     * qui modifie le solde.
     */
    @Transactional
    public Wallet findOwnedWalletForUpdateOrThrow(User currentUser, Long walletId) {
        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable"));

        if (!wallet.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce portefeuille ne vous appartient pas");
        }
        if (!wallet.isActive()) {
            throw new WalletInactiveException();
        }
        return wallet;
    }
}
