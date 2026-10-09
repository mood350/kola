package com.kola.backend.modules.admin.dto.console;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.ScheduleFrequency;
import com.kola.backend.common.enums.ScheduledTaskStatus;
import com.kola.backend.common.enums.ScheduledTaskType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConsoleScheduledTask(UUID id,
                                   ScheduledTaskType type,
                                   ScheduleFrequency frequency,
                                   BigDecimal amount,
                                   Currency currency,
                                   String description,
                                   ScheduledTaskStatus status,
                                   Instant nextRunAt,
                                   String lastFailureReason) {
}
