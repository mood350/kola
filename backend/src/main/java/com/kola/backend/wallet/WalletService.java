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

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "XOF", "GHS", "NGN", "USD", "EUR"
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
            throw new UnsupportedCurrencyException("La devise '" + currency + "' n'est pas supportée par Kola.");
        }

        if (walletRepository.existsByOwnerIdAndCurrency(currentUser.getId(), currency)) {
            throw new IllegalStateException("Vous possédez déjà un portefeuille en " + currency);
        }

        Wallet wallet = Wallet.builder()
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(currentUser)
                .build();

        return WalletResponse.fromEntity(walletRepository.save(wallet));
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════════════════

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
     * Récupère un wallet AVEC verrou pessimiste de manière 100% sécurisée.
     * Le verrou n'est posé en base de données QUE si le wallet appartient à l'utilisateur.
     */
    @Transactional
    public Wallet findOwnedWalletForUpdateOrThrow(User currentUser, Long walletId) {
        // CORRECTION IDOR : On filtre par owner.id directement dans la requête verrouillée.
        Wallet wallet = walletRepository.findOwnedWalletForUpdate(walletId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable")); // Message volontairement vague pour ne pas leak l'existence d'un wallet d'un autre user

        if (!wallet.isActive()) {
            throw new WalletInactiveException();
        }

        return wallet;
    }
}