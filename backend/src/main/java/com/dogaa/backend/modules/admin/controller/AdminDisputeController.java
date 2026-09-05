package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.DisputeDetailResponse;
import com.dogaa.backend.modules.admin.dto.DisputeResponse;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.admin.service.AdminDisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Claims and chargebacks (BACKEND.md 9). Raw DTOs, no envelope.
 *
 * <p>Every route is addressed by the disputed transaction's public reference, which is the id the
 * console and the customer both quote.
 */
@RestController
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_DISPUTES')")
@Tag(name = "Admin — litiges", description = "Réclamations et chargebacks à double validation")
@SecurityRequirement(name = "bearerAuth")
public class AdminDisputeController {

    private final AdminDisputeService adminDisputeService;

    @GetMapping
    @Operation(summary = "File des réclamations, plus récentes d'abord")
    public List<DisputeResponse> list() {
        return adminDisputeService.list();
    }

    @GetMapping("/{ref}")
    @Operation(summary = "Détail du chargeback d'un litige")
    public DisputeDetailResponse detail(@PathVariable String ref) {
        return adminDisputeService.detail(ref);
    }

    @PostMapping("/{ref}/chargeback")
    @Operation(summary = "Lancer la procédure de chargeback")
    public DisputeResponse chargeback(@AuthenticationPrincipal CurrentAdmin admin,
                                      @PathVariable String ref) {
        return adminDisputeService.startChargeback(admin, ref);
    }

    @PostMapping("/{ref}/reject")
    @Operation(summary = "Classer le litige sans suite")
    public DisputeResponse reject(@AuthenticationPrincipal CurrentAdmin admin,
                                  @PathVariable String ref) {
        return adminDisputeService.reject(admin, ref);
    }

    @PostMapping("/{ref}/validate")
    @Operation(summary = "Valider le chargeback — refusé si le même admin a déjà validé")
    public DisputeDetailResponse validate(@AuthenticationPrincipal CurrentAdmin admin,
                                          @PathVariable String ref) {
        return adminDisputeService.validate(admin, ref);
    }
}
