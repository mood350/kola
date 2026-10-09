package com.kola.backend.modules.admin.controller;

import com.kola.backend.modules.admin.dto.FeeConfigResponse;
import com.kola.backend.modules.admin.dto.MerchantResponse;
import com.kola.backend.modules.admin.dto.UpdateFeesRequest;
import com.kola.backend.modules.admin.dto.UpdateMerchantStatusRequest;
import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.modules.admin.service.AdminConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Fees and partner merchants (BACKEND.md 10). Raw DTOs, no envelope. */
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_CONFIG')")
@Tag(name = "Admin — configuration", description = "Frais, commissions et marchands partenaires")
@SecurityRequirement(name = "bearerAuth")
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @GetMapping("/fees")
    @Operation(summary = "Grille de frais par palier KYC")
    public List<FeeConfigResponse> fees() {
        return adminConfigService.fees();
    }

    /** Super-admin only — checked in the service, where the role is known. */
    @PutMapping("/fees")
    @Operation(summary = "Remplacer la grille de frais — super-admin, versionnée")
    public List<FeeConfigResponse> updateFees(@AuthenticationPrincipal CurrentAdmin admin,
                                              @Valid @RequestBody UpdateFeesRequest request) {
        return adminConfigService.updateFees(admin, request.fees());
    }

    @GetMapping("/merchants")
    @Operation(summary = "Marchands partenaires, par ordre alphabétique")
    public List<MerchantResponse> merchants() {
        return adminConfigService.merchants();
    }

    @PatchMapping("/merchants/{id}/status")
    @Operation(summary = "Activer, suspendre ou approuver un marchand")
    public MerchantResponse updateMerchantStatus(@AuthenticationPrincipal CurrentAdmin admin,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody UpdateMerchantStatusRequest request) {
        return adminConfigService.updateMerchantStatus(admin, id, request.status());
    }
}
