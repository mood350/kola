package com.kola.backend.admin;

import java.math.BigDecimal;

public record AdminMetric(
        String label,
        long count,
        BigDecimal amount
) {
}
