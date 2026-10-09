package com.kola.backend.modules.scheduling.repository;

import com.kola.backend.common.enums.ScheduledTaskStatus;
import com.kola.backend.modules.scheduling.entity.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {

    List<ScheduledTask> findByStatusAndNextRunAtLessThanEqual(ScheduledTaskStatus status, Instant now);

    List<ScheduledTask> findByUserIdOrderByNextRunAtAsc(UUID userId);
}
