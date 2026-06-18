package com.kola.backend.vault;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VaultRepository extends JpaRepository<Vault, Long> {

    // Tous les coffres d'un utilisateur
    List<Vault> findByOwnerId(Long ownerId);

    // Coffres actifs d'un utilisateur (pour calculer le lockedBalance)
    List<Vault> findByOwnerIdAndStatus(Long ownerId, VaultStatus status);

    long countByStatus(VaultStatus status);

    @Query("SELECT COALESCE(SUM(v.currentAmount), 0) FROM Vault v WHERE v.status = com.kola.backend.vault.VaultStatus.ACTIVE")
    BigDecimal sumActiveLockedAmount();
}
