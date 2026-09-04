package com.dogaa.backend.modules.audit.controller;

import com.dogaa.backend.config.AuditProperties;
import com.dogaa.backend.modules.audit.dto.AuditLogEntryResponse;
import com.dogaa.backend.modules.audit.dto.ComplianceReportResponse;
import com.dogaa.backend.modules.audit.dto.ExportResponse;
import com.dogaa.backend.modules.audit.entity.AuditLogEntry;
import com.dogaa.backend.modules.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Compliance and audit (BACKEND.md 11). DTOs are returned unwrapped, as the front-end expects.
 *
 * <p>Read-only by design: there is no endpoint to edit or delete an entry, and there should never
 * be one.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_AUDIT')")
@Tag(name = "Admin — audit", description = "Journal des actions sensibles")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;
    private final AuditProperties auditProperties;

    @GetMapping("/log")
    @Operation(summary = "Journal d'audit, plus récent d'abord — filtres optionnels")
    public List<AuditLogEntryResponse> log(@RequestParam(required = false) String admin,
                                           @RequestParam(required = false) String module,
                                           @RequestParam(required = false) Instant from,
                                           @RequestParam(required = false) Instant to) {
        return auditService.search(admin, module, from, to).stream()
                .map(AuditController::toResponse)
                .toList();
    }

    @PostMapping("/log/export")
    @Operation(summary = "Export du journal — génération non implémentée, renvoie url: null")
    public ExportResponse exportLog() {
        log.info("Audit log export requested; no file generation is wired yet");
        return new ExportResponse(null);
    }

    @GetMapping("/reports")
    @Operation(summary = "Rapports de conformité disponibles")
    public List<ComplianceReportResponse> reports() {
        return auditProperties.getReports().stream()
                .map(report -> new ComplianceReportResponse(
                        report.getId(), report.getName(), report.getPeriod()))
                .toList();
    }

    @PostMapping("/reports/{id}/export")
    @Operation(summary = "Export d'un rapport — génération non implémentée, renvoie url: null")
    public ExportResponse exportReport(@PathVariable String id) {
        log.info("Compliance report {} export requested; no file generation is wired yet", id);
        return new ExportResponse(null);
    }

    private static AuditLogEntryResponse toResponse(AuditLogEntry entry) {
        return new AuditLogEntryResponse(
                entry.getId().toString(),
                entry.getActorName(),
                entry.getAction(),
                entry.getDiff(),
                entry.getCreatedAt().toString());
    }
}
