package com.kola.backend.vault;

/**
 * Statut d'un coffre-fort numérique.
 *
 * ACTIVE   → Coffre en cours, fonds bloqués (impossible de retirer)
 * UNLOCKED → Date atteinte ou objectif rempli, fonds disponibles
 * CLOSED   → Coffre fermé manuellement par l'utilisateur avant échéance
 */
public enum VaultStatus {
    ACTIVE,
    UNLOCKED,
    CLOSED
}
