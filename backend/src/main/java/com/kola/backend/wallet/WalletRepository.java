package com.kola.backend.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Trouver tous les wallets d'un utilisateur
    List<Wallet> findByOwnerId(Long ownerId);

    // Trouver le wallet d'un user pour une devise spécifique
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByOwnerIdAndCurrency(Long ownerId, String currency);

    // Vérifier si l'utilisateur a déjà un wallet pour cette devise
    boolean existsByOwnerIdAndCurrency(Long ownerId, String currency);
}
