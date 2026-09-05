package com.dogaa.backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Compliance reports offered to the back-office.
 *
 * <p>A configured catalogue rather than a table: the list of regulatory reports is decided by the
 * compliance team, not by users, and it changes with the regulation. Generating the files
 * themselves is a separate job that does not exist yet.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    private List<Report> reports = List.of(
            new Report("aml-monthly", "Rapport mensuel AML", "Mensuel"),
            new Report("suspicion-declarations", "Déclarations de soupçon", "À la demande"),
            new Report("suspicious-thresholds", "Seuils de transactions suspectes", "Trimestriel"));

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report {
        private String id;
        private String name;
        private String period;
    }
}
