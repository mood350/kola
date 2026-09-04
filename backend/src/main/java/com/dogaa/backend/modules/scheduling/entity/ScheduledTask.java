package com.dogaa.backend.modules.scheduling.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskStatus;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledTask extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledTaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleFrequency frequency;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "beneficiary_reference", nullable = false)
    private String beneficiaryReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledTaskStatus status = ScheduledTaskStatus.ACTIVE;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "max_occurrences")
    private Integer maxOccurrences;

    @Column(name = "occurrences_completed", nullable = false)
    private int occurrencesCompleted = 0;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_failure_reason")
    private String lastFailureReason;
}
