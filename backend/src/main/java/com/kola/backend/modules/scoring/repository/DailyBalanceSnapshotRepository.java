package com.kola.backend.modules.scoring.repository;

import com.kola.backend.modules.scoring.entity.DailyBalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyBalanceSnapshotRepository extends JpaRepository<DailyBalanceSnapshot, UUID> {

    List<DailyBalanceSnapshot> findByUserIdAndSnapshotDateGreaterThanEqual(UUID userId, LocalDate from);

    Optional<DailyBalanceSnapshot> findByUserIdAndSnapshotDate(UUID userId, LocalDate snapshotDate);

    @Modifying
    @Query("delete from DailyBalanceSnapshot s where s.snapshotDate < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDate cutoff);
}
