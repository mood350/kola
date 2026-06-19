package com.kola.backend.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByOwnerId(Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.owner.id = :ownerId AND w.currency = :currency")
    Optional<Wallet> findByOwnerIdAndCurrency(@Param("ownerId") Long ownerId, @Param("currency") String currency);

    @Query("SELECT w FROM Wallet w WHERE w.owner.id = :ownerId AND w.currency = :currency")
    Optional<Wallet> findByOwnerIdAndCurrencyNoLock(@Param("ownerId") Long ownerId, @Param("currency") String currency);

    // SÉCURITÉ : Gardé pour les cas où le système interne (sans contexte utilisateur) doit verrouiller un wallet
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    // NOUVEAU : Verrouillage 100% sécurisé. On ne pose le lock QUE si le wallet appartient au user.
    // Si l'utilisateur n'est pas le propriétaire, la requête renvoie juste Optional.empty() (pas de lock posé).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId AND w.owner.id = :ownerId")
    Optional<Wallet> findOwnedWalletForUpdate(@Param("walletId") Long walletId, @Param("ownerId") Long ownerId);

    boolean existsByOwnerIdAndCurrency(Long ownerId, String currency);

    long countByActiveTrue();

    @Query("SELECT w.currency, COALESCE(SUM(w.balance), 0), COALESCE(SUM(w.lockedBalance), 0), COUNT(w) FROM Wallet w GROUP BY w.currency ORDER BY SUM(w.balance) DESC")
    List<Object[]> summarizeByCurrency();

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w WHERE w.active = true")
    BigDecimal sumActiveBalances();
}