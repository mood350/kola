package com.kola.backend.aml;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Console conformité LAB-FT.
 *
 * Monté sous /api/admin/** : SecurityConfig y exige déjà l'autorité ADMIN.
 * C'est délibéré et non négociable — exposer ces alertes au client concerné
 * constituerait une divulgation illicite (« tipping off »).
 */
@RestController
@RequestMapping("/api/admin/aml")
@RequiredArgsConstructor
public class AmlController {

    private final AmlAlertService amlAlertService;

    /** Liste paginée, filtrable par statut et par niveau de risque. */
    @GetMapping("/alerts")
    public ResponseEntity<Page<AmlAlertResponse>> getAlerts(
            @RequestParam(required = false) AmlAlertStatus status,
            @RequestParam(required = false) AmlRiskLevel riskLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(amlAlertService.search(status, riskLevel, pageable));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<AmlAlertResponse> getAlert(@PathVariable Long id) {
        return ResponseEntity.ok(amlAlertService.getById(id));
    }

    /** Volumétrie pour un tableau de bord conformité. */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(amlAlertService.overview());
    }

    /** Traitement d'une alerte par l'analyste (classement ou confirmation). */
    @PostMapping("/alerts/{id}/review")
    public ResponseEntity<AmlAlertResponse> review(
            @AuthenticationPrincipal User analyst,
            @PathVariable Long id,
            @RequestBody @Valid ReviewRequest request
    ) {
        return ResponseEntity.ok(
                amlAlertService.review(id, analyst, request.status(), request.notes()));
    }

    public record ReviewRequest(
            @NotNull(message = "Le nouveau statut est obligatoire")
            AmlAlertStatus status,
            String notes
    ) {
    }
}
