package com.dogaa.backend.modules.transaction.repository;

import com.dogaa.backend.modules.transaction.entity.FeeScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeScheduleRepository extends JpaRepository<FeeScheduleEntry, UUID> {

    List<FeeScheduleEntry> findByVersionNumber(long versionNumber);

    @Query("select max(f.versionNumber) from FeeScheduleEntry f")
    Optional<Long> findLatestVersionNumber();
}
