package com.dogaa.backend.modules.scheduling.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import com.dogaa.backend.common.enums.Biller;
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

    /**
     * The vault this schedule spends from.
     *
     * <p>Required for everything except vault and Bankivi deposits, whose source is the current
     * account by nature. A schedule used to debit the current account directly, which meant money left the
     * everyday balance on a date the user had chosen weeks earlier and no longer had in mind —
     * exactly the surprise a wallet must not produce. Naming a vault makes the money set aside
     * on purpose, and visibly short if it is.
     */
    @Column(name = "funding_vault_id")
    private UUID fundingVaultId;

    /** Set for BILL_PAYMENT: which biller, and therefore which identifier was asked for. */
    @Enumerated(EnumType.STRING)
    @Column(name = "biller", length = 32)
    private Biller biller;

    /**
     * Day of the month a monthly schedule falls on, 1-31, kept apart from {@code nextRunAt}.
     *
     * <p>Storing the intent rather than deriving it from the last run is what stops the date from
     * drifting: a schedule set for the 31st must come back to the 31st in March after landing on
     * the 28th in February, and a run that is retried a day late must not move the whole series.
     */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

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
