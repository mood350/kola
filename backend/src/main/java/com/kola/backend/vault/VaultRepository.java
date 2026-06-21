package com.kola.backend.vault;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VaultRepository extends JpaRepository<Vault, Long> {

    // Tous les coffres d'un utilisateur
    List<Vault> findByOwnerId(Long ownerId);

    // Coffres actifs d'un utilisateur (pour calculer le lockedBalance)
    List<Vault> findByOwnerIdAndStatus(Long ownerId, VaultStatus status);

    long countByStatus(VaultStatus status);

    @Query("SELECT COALESCE(SUM(v.currentAmount), 0) FROM Vault v WHERE v.status = com.kola.backend.vault.VaultStatus.ACTIVE")
    BigDecimal sumActiveLockedAmount();

    // Sécurité maximale : TOUS les appels à un Vault par ID passeront par ici
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vault v WHERE v.id = :id")
    Optional<Vault> findByIdForUpdate(@Param("id") Long id);
}