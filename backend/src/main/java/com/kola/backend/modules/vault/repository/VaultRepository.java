package com.kola.backend.modules.vault.repository;

import com.kola.backend.modules.vault.entity.Vault;
import com.kola.backend.modules.vault.entity.VaultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VaultRepository extends JpaRepository<Vault, UUID> {

    List<Vault> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<Vault> findByIdAndOwnerId(UUID id, UUID ownerId);

    long countByOwnerIdAndStatus(UUID ownerId, VaultStatus status);

    /** Oldest open vault: what a forced closure without a target acts on. */
    Optional<Vault> findFirstByOwnerIdAndStatusOrderByCreatedAtAsc(UUID ownerId, VaultStatus status);
}
