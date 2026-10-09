package com.kola.backend.modules.dispute.controller;

import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.modules.dispute.dto.DisputeDetailResponse;
import com.kola.backend.modules.dispute.dto.DisputeResponse;
import com.kola.backend.modules.dispute.service.DisputeService;
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

/** Disputes and chargebacks (BACKEND.md 9). Raw DTOs, no envelope. */
@RestController
@RequestMapping("/api/v1/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_DISPUTES')")
@Tag(name = "Admin — litiges", description = "Contestations et chargebacks à double validation")
@SecurityRequirement(name = "bearerAuth")
public class AdminDisputeController {

    private final DisputeService disputeService;

    @GetMapping
    @Operation(summary = "Tous les litiges, plus récents d'abord")
    public List<DisputeResponse> list() {
        return disputeService.list();
    }

    @GetMapping("/{ref}")
    @Operation(summary = "Détail d'un litige et état de ses validations")
    public DisputeDetailResponse detail(@PathVariable String ref) {
        return disputeService.detail(ref);
    }

    @PostMapping("/{ref}/chargeback")
    @Operation(summary = "Proposer un chargeback — rien ne bouge avant les validations")
    public DisputeResponse chargeback(@AuthenticationPrincipal CurrentAdmin admin,
                                      @PathVariable String ref) {
        return disputeService.proposeChargeback(admin, ref);
    }

    @PostMapping("/{ref}/reject")
    @Operation(summary = "Classer le litige sans suite")
    public DisputeResponse reject(@AuthenticationPrincipal CurrentAdmin admin,
                                  @PathVariable String ref) {
        return disputeService.reject(admin, ref);
    }

    @PostMapping("/{ref}/validate")
    @Operation(summary = "Valider le chargeback — refusé si vous l'avez déjà validé")
    public DisputeDetailResponse validate(@AuthenticationPrincipal CurrentAdmin admin,
                                          @PathVariable String ref) {
        return disputeService.validate(admin, ref);
    }
}
