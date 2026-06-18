package com.kola.backend.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Trouver tous les wallets d'un utilisateur
    List<Wallet> findByOwnerId(Long ownerId);

    // Trouver le wallet d'un user pour une devise spécifique.
    // BUG CORRIGÉ : la requête précédente était "WHERE w.id = :id" avec une
    // méthode prenant (ownerId, currency) — :id n'existait pas dans la
    // signature (échec au démarrage de l'app) et même corrigé naïvement en
    // ":id", la requête n'aurait jamais filtré par owner ni devise, exposant
    // potentiellement le wallet d'un autre utilisateur (IDOR).
    // Le verrou PESSIMISTIC_WRITE est conservé : indispensable pour éviter
    // les race conditions sur le solde lors de transferts concurrents
    // (lecture-modification-écriture du balance).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.owner.id = :ownerId AND w.currency = :currency")
    Optional<Wallet> findByOwnerIdAndCurrency(@Param("ownerId") Long ownerId, @Param("currency") String currency);

    // Variante non verrouillante, pour les lectures simples (consultation de solde)
    // qui n'ont pas besoin de bloquer la ligne en BDD.
    @Query("SELECT w FROM Wallet w WHERE w.owner.id = :ownerId AND w.currency = :currency")
    Optional<Wallet> findByOwnerIdAndCurrencyNoLock(@Param("ownerId") Long ownerId, @Param("currency") String currency);

    // Récupération verrouillée par ID (pour les opérations de débit/crédit directes)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    // Vérifier si l'utilisateur a déjà un wallet pour cette devise
    boolean existsByOwnerIdAndCurrency(Long ownerId, String currency);
}
