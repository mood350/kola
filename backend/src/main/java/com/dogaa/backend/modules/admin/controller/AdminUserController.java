package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.ClientUserResponse;
import com.dogaa.backend.modules.admin.dto.KycSubmissionResponse;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Customer accounts and the KYC queue (BACKEND.md 6). Raw DTOs, no envelope.
 *
 * <p>Note the ordering of the mappings: {@code /kyc-queue} is declared before {@code /{id}} so the
 * literal path is not swallowed by the UUID placeholder.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_USERS')")
@Tag(name = "Admin — utilisateurs", description = "Comptes clients et file KYC")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/kyc-queue")
    @Operation(summary = "Documents KYC en attente de revue, plus anciens d'abord")
    public List<KycSubmissionResponse> kycQueue() {
        return adminUserService.kycQueue();
    }

    @PostMapping("/kyc-queue/{submissionId}/approve")
    @Operation(summary = "Approuver une soumission KYC — le niveau est recalculé")
    public ResponseEntity<Void> approveKyc(@AuthenticationPrincipal CurrentAdmin admin,
                                           @PathVariable UUID submissionId) {
        adminUserService.approveKyc(admin, submissionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/kyc-queue/{submissionId}/reject")
    @Operation(summary = "Rejeter une soumission KYC")
    public ResponseEntity<Void> rejectKyc(@AuthenticationPrincipal CurrentAdmin admin,
                                          @PathVariable UUID submissionId) {
        adminUserService.rejectKyc(admin, submissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Tous les comptes clients")
    public List<ClientUserResponse> listUsers() {
        return adminUserService.listUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Un compte client")
    public ClientUserResponse getUser(@PathVariable UUID id) {
        return adminUserService.getUser(id);
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Débloquer un compte gelé")
    public ClientUserResponse unblock(@AuthenticationPrincipal CurrentAdmin admin,
                                      @PathVariable UUID id) {
        return adminUserService.unblock(admin, id);
    }

    @PostMapping("/{id}/force-close-vault")
    @Operation(summary = "Fermer d'office le plus ancien coffre ouvert du client")
    public ClientUserResponse forceCloseVault(@AuthenticationPrincipal CurrentAdmin admin,
                                              @PathVariable UUID id) {
        return adminUserService.forceCloseVault(admin, id);
    }
}
