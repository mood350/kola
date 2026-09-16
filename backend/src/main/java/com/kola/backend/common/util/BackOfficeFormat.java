package com.kola.backend.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Formatting for the back-office.
 *
 * <p>The admin front-end renders every value verbatim and holds no formatting logic, so the strings
 * are built here. Shared by every module that answers the console, so "how an amount looks" is
 * defined once rather than once per screen.
 */
public final class BackOfficeFormat {

    private BackOfficeFormat() {
    }

    /** "100 000 XOF" — narrow no-break space between groups, as French typography expects. */
    public static String amount(BigDecimal value, String currency) {
        if (value == null) {
            return "0 " + currency;
        }
        String digits = value.setScale(0, RoundingMode.HALF_UP).toPlainString();
        boolean negative = digits.startsWith("-");
        if (negative) {
            digits = digits.substring(1);
        }
        StringBuilder grouped = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            grouped.append(digits.charAt(i));
            if (++count % 3 == 0 && i > 0) {
                grouped.append(' ');
            }
        }
        return (negative ? "-" : "") + grouped.reverse() + " " + currency;
    }

    /** Compact form for headline tiles: "12,4 M XOF", "850 k XOF". */
    public static String compactAmount(BigDecimal value, String currency) {
        if (value == null) {
            return "0 " + currency;
        }
        double units = value.doubleValue();
        if (Math.abs(units) >= 1_000_000) {
            return String.format("%.1f M %s", units / 1_000_000, currency).replace('.', ',');
        }
        if (Math.abs(units) >= 1_000) {
            return String.format("%.0f k %s", units / 1_000, currency);
        }
        return amount(value, currency);
    }

    /** How long the account has existed, in the coarsest unit that still says something. */
    public static String age(Instant createdAt) {
        if (createdAt == null) {
            return "—";
        }
        long days = ChronoUnit.DAYS.between(createdAt, Instant.now());
        if (days < 1) {
            return "Aujourd'hui";
        }
        if (days < 31) {
            return days + (days == 1 ? " jour" : " jours");
        }
        long months = days / 30;
        if (months < 24) {
            return months + " mois";
        }
        return (months / 12) + " ans";
    }

    /** Lending rate as the tier editor shows it: "7 %/mois". */
    public static String monthlyRate(BigDecimal percentPerMonth) {
        return percent(percentPerMonth).replace(" %", " %/mois");
    }

    public static String percent(BigDecimal value) {
        if (value == null) {
            return "0 %";
        }
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %";
    }

    /** Signed change for a metric tile: "+8,0 %". */
    public static String signedPercent(BigDecimal value) {
        String rendered = percent(value == null ? BigDecimal.ZERO : value.abs());
        return (value != null && value.signum() < 0 ? "−" : "+") + rendered;
    }

    /** Share of {@code part} in {@code whole}, 0-100, safe when nothing has happened yet. */
    public static BigDecimal share(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0 || part == null) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_UP);
    }
}
