package com.kola.backend.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Historique complet d'un wallet (toutes transactions)
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    // Historique des envois d'un utilisateur
    List<Transaction> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    // Historique des réceptions d'un utilisateur
    List<Transaction> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    // Récupérer une transaction par sa référence unique
    Optional<Transaction> findByReference(String reference);

    // Toutes les transactions en attente (pour un job de vérification)
    List<Transaction> findByStatus(TransactionStatus status);
}
