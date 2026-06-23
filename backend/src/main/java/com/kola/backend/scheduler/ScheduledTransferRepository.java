package com.kola.backend.scheduler;

import com.kola.backend.scheduler.ScheduledTransfer.ScheduledStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {

    @Query("SELECT st FROM ScheduledTransfer st WHERE st.status = :status AND st.nextExecutionDate <= :now")
    List<ScheduledTransfer> findDueTransfers(@Param("status") ScheduledStatus status, @Param("now") LocalDateTime now);
}