package com.kola.backend.modules.wallet.repository;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.modules.wallet.dto.WalletAggregate;
import com.kola.backend.modules.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByOwnerId(UUID ownerId);

    Optional<Wallet> findByOwnerIdAndCurrencyAndType(UUID ownerId, Currency currency, WalletType type);

    boolean existsByOwnerIdAndCurrencyAndType(UUID ownerId, Currency currency, WalletType type);

    List<Wallet> findByOwnerIdAndType(UUID ownerId, WalletType type);

    /**
     * Row-locking read for money moves: two concurrent debits on the same wallet queue
     * on the database instead of both reading the pre-debit balance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Wallet count and total available/locked balance, one row per currency
     * (KOLA.md 4.5: "solde global"). Consumed by kola-admin through
     * {@code WalletService}, never directly.
     */
    @Query("""
            select new com.kola.backend.modules.wallet.dto.WalletAggregate(
                w.currency, count(w), coalesce(sum(w.availableBalance), 0), coalesce(sum(w.lockedBalance), 0))
            from Wallet w
            where w.status = com.kola.backend.modules.wallet.entity.WalletStatus.ACTIVE
            group by w.currency
            """)
    List<WalletAggregate> aggregateByCurrency();
}
