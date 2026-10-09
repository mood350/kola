package com.kola.backend.modules.auth.repository;

import com.kola.backend.modules.auth.entity.OtpCode;
import com.kola.backend.modules.auth.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findFirstByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String phone, OtpPurpose purpose);

    Optional<OtpCode> findByTokenHash(String tokenHash);

    /** Called before issuing a new challenge: only one live code per phone and purpose. */
    @Modifying
    @Query("update OtpCode o set o.consumedAt = :now "
            + "where o.phone = :phone and o.purpose = :purpose and o.consumedAt is null")
    int consumeAllForPhone(@Param("phone") String phone,
                           @Param("purpose") OtpPurpose purpose,
                           @Param("now") Instant now);

    @Modifying
    @Query("delete from OtpCode o where o.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
