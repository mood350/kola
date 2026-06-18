package com.kola.backend.credit;

public enum LoanStatus {
    PENDING,     // En attente de validation
    APPROVED,    // Approuvé, en attente de déboursement
    REJECTED,    // Refusé (score insuffisant ou règle métier)
    DISBURSED,   // Fonds versés sur le wallet de l'emprunteur
    REPAID,      // Entièrement remboursé
    DEFAULTED    // En défaut de paiement (échéance dépassée)
}
