package com.kola.backend.modules.qr.repository;

import com.kola.backend.modules.qr.entity.QrCode;
import com.kola.backend.modules.qr.entity.QrCodeStatus;
import com.kola.backend.modules.qr.entity.QrCodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, UUID> {

    Optional<QrCode> findByCode(String code);

    /** Loading by code and owner in one query is what keeps "my code" from meaning anyone's. */
    Optional<QrCode> findByCodeAndOwnerId(String code, UUID ownerId);

    Optional<QrCode> findFirstByOwnerIdAndTypeAndStatus(UUID ownerId, QrCodeType type,
                                                        QrCodeStatus status);

    List<QrCode> findByOwnerIdAndTypeOrderByCreatedAtDesc(UUID ownerId, QrCodeType type);

    boolean existsByCode(String code);
}
