package com.kola.backend.transaction;

/**
 * Statut d'une transaction.
 *
 * PENDING   → En attente de confirmation (auprès de l'opérateur Mobile Money)
 * SUCCESS   → Transaction complétée avec succès
 * FAILED    → Transaction échouée (solde insuffisant, réseau indisponible, etc.)
 * CANCELLED → Annulée par l'utilisateur avant traitement
 * REFUNDED  → Remboursée après une transaction échouée
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED
}
