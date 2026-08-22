package com.kola.backend.merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByMerchantCode(String merchantCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Merchant m WHERE m.merchantCode = :code")
    Optional<Merchant> findByMerchantCodeForUpdate(@Param("code") String merchantCode);
}
