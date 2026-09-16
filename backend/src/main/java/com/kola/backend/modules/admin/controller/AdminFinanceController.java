package com.kola.backend.modules.admin.controller;

import com.kola.backend.modules.admin.dto.LiquidityBucketResponse;
import com.kola.backend.modules.admin.dto.OperatorStatusResponse;
import com.kola.backend.modules.admin.dto.RevenueResponse;
import com.kola.backend.modules.admin.service.AdminFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Liquidity, revenue and operator reconciliation (BACKEND.md 8). Read-only. */
@RestController
@RequestMapping("/api/v1/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_FINANCE')")
@Tag(name = "Admin — suivi financier", description = "Liquidité, revenus et réconciliation")
@SecurityRequirement(name = "bearerAuth")
public class AdminFinanceController {

    private final AdminFinanceService adminFinanceService;

    @GetMapping("/liquidity")
    @Operation(summary = "Répartition de la liquidité")
    public List<LiquidityBucketResponse> liquidity() {
        return adminFinanceService.liquidity();
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenus par source et total")
    public RevenueResponse revenue() {
        return adminFinanceService.revenue();
    }

    @GetMapping("/operator-reconciliation")
    @Operation(summary = "État de réconciliation par opérateur — vide tant qu'aucune API opérateur n'est branchée")
    public List<OperatorStatusResponse> operatorReconciliation() {
        return adminFinanceService.operatorReconciliation();
    }
}
