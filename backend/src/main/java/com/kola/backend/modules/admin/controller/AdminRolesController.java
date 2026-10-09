package com.kola.backend.modules.admin.controller;

import com.kola.backend.modules.admin.dto.AdminAccountResponse;
import com.kola.backend.modules.admin.dto.UpdatePermissionsRequest;
import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.modules.admin.service.AdminAccountService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Staff and their permissions (BACKEND.md 12). Super-admin only, enforced server-side. */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODULE_ROLES')")
@Tag(name = "Admin — rôles", description = "Gestion des comptes du back-office")
@SecurityRequirement(name = "bearerAuth")
public class AdminRolesController {

    private final AdminAccountService adminAccountService;

    @GetMapping("/admins")
    @Operation(summary = "Liste des comptes administrateurs")
    public List<AdminAccountResponse> listAdmins() {
        return adminAccountService.listAdmins();
    }

    @PatchMapping("/admins/{id}/permissions")
    @Operation(summary = "Modifier le rôle et le périmètre d'un administrateur")
    public AdminAccountResponse updatePermissions(@AuthenticationPrincipal CurrentAdmin actor,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody UpdatePermissionsRequest request) {
        return adminAccountService.updatePermissions(actor, id, request);
    }
}
