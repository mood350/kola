package com.kola.backend.modules.admin.controller;

import com.kola.backend.modules.admin.dto.ManualActionResponse;
import com.kola.backend.modules.admin.dto.SupportTicketResponse;
import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.modules.admin.service.AdminSupportService;
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

/** Customer tickets and manual interventions (BACKEND.md 13). Raw DTOs, no envelope. */
@RestController
@RequestMapping("/api/v1/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_SUPPORT')")
@Tag(name = "Admin — support", description = "Tickets et traçabilité des actions manuelles")
@SecurityRequirement(name = "bearerAuth")
public class AdminSupportController {

    private final AdminSupportService adminSupportService;

    @GetMapping("/tickets")
    @Operation(summary = "File des tickets, plus anciens d'abord")
    public List<SupportTicketResponse> tickets() {
        return adminSupportService.tickets();
    }

    @GetMapping("/manual-actions")
    @Operation(summary = "Actions manuelles récentes des agents")
    public List<ManualActionResponse> manualActions() {
        return adminSupportService.manualActions();
    }

    @PostMapping("/tickets/{ref}/take-charge")
    @Operation(summary = "Prendre en charge un ticket ouvert")
    public SupportTicketResponse takeCharge(@AuthenticationPrincipal CurrentAdmin admin,
                                            @PathVariable String ref) {
        return adminSupportService.takeCharge(admin, ref);
    }

    @PostMapping("/tickets/{ref}/resolve")
    @Operation(summary = "Marquer un ticket résolu")
    public SupportTicketResponse resolve(@AuthenticationPrincipal CurrentAdmin admin,
                                         @PathVariable String ref) {
        return adminSupportService.resolve(admin, ref);
    }
}
