package com.kola.backend.transaction;

/**
 * Type de mouvement financier dans Kola.
 * DEPOSIT          → Rechargement du wallet (depuis Mobile Money)
 * WITHDRAWAL       → Retrait du wallet (vers Mobile Money)
 * TRANSFER_OUT     → Envoi d'argent à un bénéficiaire
 * TRANSFER_IN      → Réception d'un transfert
 * VAULT_LOCK       → Blocage de fonds dans un coffre-fort
 * VAULT_UNLOCK     → Déblocage de fonds depuis un coffre-fort
 * FEE              → Prélèvement de frais de transaction
 * MERCHANT_PAYMENT → Paiement à un marchand (scan QR)
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_OUT,
    TRANSFER_IN,
    VAULT_LOCK,
    VAULT_UNLOCK,
    FEE,
    SCHEDULED_TRANSFER, // <--- AJOUTER ICI
    LOAN_DISBURSEMENT,
    LOAN_REPAYMENT,
    MERCHANT_PAYMENT
}
