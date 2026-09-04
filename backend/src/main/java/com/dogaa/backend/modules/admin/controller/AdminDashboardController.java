package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.AlertResponse;
import com.dogaa.backend.modules.admin.dto.ChartPointResponse;
import com.dogaa.backend.modules.admin.dto.LoanBookSummaryResponse;
import com.dogaa.backend.modules.admin.dto.MetricResponse;
import com.dogaa.backend.modules.admin.service.AdminDashboardMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dashboard figures (BACKEND.md 5). Raw DTOs, no envelope. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_DASHBOARD')")
@Tag(name = "Admin — dashboard", description = "Indicateurs du back-office")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final AdminDashboardMetricsService metricsService;

    @GetMapping("/metrics")
    @Operation(summary = "Les 5 tuiles, dans l'ordre d'affichage")
    public List<MetricResponse> metrics() {
        return metricsService.metrics();
    }

    @GetMapping("/transaction-volume")
    @Operation(summary = "Volume quotidien sur 14 ou 30 jours")
    public List<ChartPointResponse> transactionVolume(
            @RequestParam(defaultValue = "14d") String period) {
        return metricsService.transactionVolume(period);
    }

    @GetMapping("/alerts")
    @Operation(summary = "Alertes calculées depuis l'état réel — liste vide si tout va bien")
    public List<AlertResponse> alerts() {
        return metricsService.alerts();
    }

    @GetMapping("/loan-book-summary")
    @Operation(summary = "Encours, allocation, taux de défaut et croissance")
    public LoanBookSummaryResponse loanBookSummary() {
        return metricsService.loanBookSummary();
    }
}
