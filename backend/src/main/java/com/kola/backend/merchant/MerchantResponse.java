package com.kola.backend.merchant;

/**
 * DTO de sortie pour un marchand — jamais le solde, seulement de quoi
 * confirmer au payeur qui il s'apprête à payer avant de valider le montant.
 */
public record MerchantResponse(
        Long id,
        String name,
        String category,
        String merchantCode
) {
    public static MerchantResponse fromEntity(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getCategory(),
                merchant.getMerchantCode()
        );
    }
}
