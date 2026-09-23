package com.kola.backend.common.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * "Il y a 2 h", "Hier", "12/03/2026".
 *
 * <p>The admin console prints these strings verbatim and holds no formatting logic of its own, so
 * every screen that shows an age has to build the same phrasing — which is why it is written once
 * here rather than in each controller.
 */
public final class RelativeTime {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);

    private RelativeTime() {
    }

    /** Capitalised, for use on its own: "Il y a 2 h". */
    public static String since(Instant when) {
        if (when == null) {
            return "—";
        }
        long minutes = ChronoUnit.MINUTES.between(when, Instant.now());
        if (minutes < 1) {
            return "À l'instant";
        }
        if (minutes < 60) {
            return "Il y a " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "Il y a " + hours + " h";
        }
        long days = hours / 24;
        if (days == 1) {
            return "Hier";
        }
        if (days < 31) {
            return "Il y a " + days + " jours";
        }
        return DATE.format(when);
    }

    /** Lower-cased for mid-sentence use: "Ouvert il y a 2 h". */
    public static String sinceInSentence(Instant when) {
        String rendered = since(when);
        return rendered.startsWith("Il y a")
                ? "il y a" + rendered.substring("Il y a".length())
                : rendered;
    }
}
