package com.kola.backend.modules.vault.controller;

import com.kola.backend.common.dto.ApiResponse;
import com.kola.backend.modules.auth.security.CurrentUser;
import com.kola.backend.modules.vault.dto.CreateVaultRequest;
import com.kola.backend.modules.vault.dto.VaultMovementResponse;
import com.kola.backend.modules.vault.dto.VaultOperationRequest;
import com.kola.backend.modules.vault.dto.UpdateVaultRequest;
import com.kola.backend.modules.vault.dto.VaultResponse;
import com.kola.backend.modules.vault.mapper.VaultMapper;
import com.kola.backend.modules.vault.service.VaultService;
import com.kola.backend.modules.wallet.mapper.WalletMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vaults")
@RequiredArgsConstructor
@Tag(name = "Vaults", description = "Programmed-savings goals with locked funds (KOLA.md 4.2)")
@SecurityRequirement(name = "bearerAuth")
public class VaultController {

    private final VaultService vaultService;
    private final VaultMapper vaultMapper;
    private final WalletMapper walletMapper;

    /** Both sides of a movement: the vault, and the wallet whose available/locked split changed. */
    private VaultMovementResponse toResponse(VaultService.VaultMovement movement) {
        return new VaultMovementResponse(
                vaultMapper.toResponse(movement.vault()),
                walletMapper.toResponse(movement.wallet()));
    }

    @GetMapping
    @Operation(summary = "List my vaults")
    public ResponseEntity<ApiResponse<List<VaultResponse>>> list(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<VaultResponse> vaults = vaultService.listVaults(currentUser.id())
                .stream().map(vaultMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(vaults));
    }

    @PostMapping
    @Operation(summary = "Create a savings vault")
    public ResponseEntity<ApiResponse<VaultResponse>> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateVaultRequest request) {
        VaultResponse vault = vaultMapper.toResponse(
                vaultService.createVault(currentUser.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Vault created", vault));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one vault")
    public ResponseEntity<ApiResponse<VaultResponse>> get(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                vaultMapper.toResponse(vaultService.getVault(currentUser.id(), id))));
    }

    /**
     * Verse depuis le compte courant vers le coffre.
     *
     * <p>Renvoie les deux nouveaux soldes — celui du coffre et celui du compte courant — parce que
     * l'opération les modifie tous les deux et que le client doit afficher les deux sans avoir à
     * rappeler {@code GET /wallets}.
     */
    @PostMapping("/{id}/deposit")
    @Operation(summary = "Verser du compte courant vers le coffre — renvoie les deux nouveaux soldes")
    public ResponseEntity<ApiResponse<VaultMovementResponse>> deposit(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody VaultOperationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit completed",
                toResponse(vaultService.deposit(currentUser.id(), id, request.amount()))));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Reverser le coffre vers le compte courant — renvoie les deux nouveaux soldes")
    public ResponseEntity<ApiResponse<VaultMovementResponse>> withdraw(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody VaultOperationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Withdrawal completed",
                toResponse(vaultService.withdraw(currentUser.id(), id, request.amount()))));
    }

    /**
     * Renames a goal or moves its target. Absent fields are left alone.
     *
     * <p>There is no balance field: money enters and leaves a vault only through a deposit, a
     * withdrawal or a scheduled payment, each of which writes a transaction. Setting a balance
     * directly would create money the ledger could not account for.
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Modifier un coffre — nom, objectif, échéance (jamais le solde)")
    public ResponseEntity<ApiResponse<VaultResponse>> update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVaultRequest request) {
        VaultResponse vault = vaultMapper.toResponse(
                vaultService.updateVault(currentUser.id(), id, request));
        return ResponseEntity.ok(ApiResponse.ok("Coffre modifié", vault));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Clôturer le coffre et libérer le solde — renvoie les deux nouveaux soldes")
    public ResponseEntity<ApiResponse<VaultMovementResponse>> close(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Vault closed",
                toResponse(vaultService.closeVault(currentUser.id(), id))));
    }
}
