package com.kola.backend.user;

/**
 * Niveau de vérification KYC (Know Your Customer) de l'utilisateur.
 *
 * TIER_0 → Inscription simple (numéro de téléphone uniquement)
 * TIER_1 → Email vérifié  (accès aux fonctionnalités de base)
 * TIER_2 → Pièce d'identité soumise (transferts limités)
 * TIER_3 → Identité validée (accès complet, transferts élevés)
 */
public enum KycLevel {
    TIER_0,
    TIER_1,
    TIER_2,
    TIER_3
}
