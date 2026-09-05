package com.dogaa.backend.modules.credit.repository;

import com.dogaa.backend.modules.credit.entity.CreditTierConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditTierConfigRepository extends JpaRepository<CreditTierConfig, UUID> {

    List<CreditTierConfig> findByVersionNumberOrderByPositionAsc(long versionNumber);

    @Query("select max(c.versionNumber) from CreditTierConfig c")
    Optional<Long> findLatestVersionNumber();
}
