package com.kola.backend.aml;

/**
 * Émis une fois qu'une transaction financière est validée.
 * On transporte des identifiants et non des entités : le consommateur
 * s'exécute après le commit, dans un autre contexte de persistance.
 */
public record TransactionCompletedEvent(Long userId, Long transactionId) {
}
