package com.kola.backend.modules.admin.dto.console;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Completed volume of one UTC day (Lomé time). A day without activity is present, at zero. */
public record ConsoleDailyVolume(LocalDate day, BigDecimal amount) {
}
