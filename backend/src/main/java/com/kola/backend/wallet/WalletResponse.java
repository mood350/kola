package com.kola.backend.wallet;

import java.math.BigDecimal;

/**
 * DTO de sortie pour un wallet.
 * On n'exposera JAMAIS l'entité JPA Wallet directement dans l'API :
 *  - évite les boucles de sérialisation (Wallet → owner → wallets → ...)
 *  - évite d'exposer des champs internes non destinés au client
 */
public record WalletResponse(
        Long id,
        String currency,
        BigDecimal balance,
        BigDecimal lockedBalance,
        BigDecimal availableBalance, // balance - lockedBalance, calculé
        boolean active
) {
    public static WalletResponse fromEntity(Wallet wallet) {
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        return new WalletResponse(
                wallet.getId(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getLockedBalance(),
                available,
                wallet.isActive()
        );
    }
}
