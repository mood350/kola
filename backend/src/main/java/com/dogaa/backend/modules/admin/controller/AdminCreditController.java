package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.CreditStatsResponse;
import com.dogaa.backend.modules.admin.dto.LoanDefaultResponse;
import com.dogaa.backend.modules.admin.dto.TierConfigResponse;
import com.dogaa.backend.modules.admin.dto.UpdateTierConfigRequest;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.admin.service.AdminCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Loan book, lending tiers and recovery (BACKEND.md 7). Raw DTOs, no envelope. */
@RestController
@RequestMapping("/api/v1/admin/credit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_CREDIT')")
@Tag(name = "Admin — crédit", description = "Portefeuille de prêts, paliers et défauts")
@SecurityRequirement(name = "bearerAuth")
public class AdminCreditController {

    private final AdminCreditService adminCreditService;

    @GetMapping("/stats")
    @Operation(summary = "Encours, taux de défaut et prêts en retard")
    public CreditStatsResponse stats() {
        return adminCreditService.stats();
    }

    @GetMapping("/tier-config")
    @Operation(summary = "Barème des paliers de prêt en vigueur")
    public List<TierConfigResponse> tierConfig() {
        return adminCreditService.tierConfig();
    }

    /**
     * Changing the terms of every future loan is a super-admin act, and the screen says so. The
     * role is checked in the service: {@code MODULE_CREDIT} alone also belongs to the credit
     * analyst, who may read this page but not rewrite the risk model.
     */
    @PutMapping("/tier-config")
    @Operation(summary = "Remplacer le barème — super-admin, versionné")
    public List<TierConfigResponse> updateTierConfig(@AuthenticationPrincipal CurrentAdmin admin,
                                                     @Valid @RequestBody UpdateTierConfigRequest request) {
        return adminCreditService.updateTierConfig(admin, request.tiers());
    }

    @GetMapping("/defaults")
    @Operation(summary = "Prêts en retard ou en défaut, plus anciens d'abord")
    public List<LoanDefaultResponse> defaults() {
        return adminCreditService.defaults();
    }

    @PostMapping("/defaults/{loanId}/remind")
    @Operation(summary = "Relancer l'emprunteur par SMS")
    public ResponseEntity<Void> remind(@AuthenticationPrincipal CurrentAdmin admin,
                                       @PathVariable UUID loanId) {
        adminCreditService.remind(admin, loanId);
        return ResponseEntity.noContent().build();
    }
}
