package com.kola.backend.vault;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaults")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @GetMapping
    public ResponseEntity<List<VaultResponse>> getMyVaults(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(vaultService.getMyVaults(currentUser));
    }

    @GetMapping("/{vaultId}")
    public ResponseEntity<VaultResponse> getVault(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long vaultId
    ) {
        return ResponseEntity.ok(vaultService.getVaultById(currentUser, vaultId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VaultResponse createVault(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid CreateVaultRequest request
    ) {
        return vaultService.createVault(currentUser, request);
    }

    @PostMapping("/{vaultId}/add-funds")
    public VaultResponse addFunds(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long vaultId,
            @RequestBody @Valid AddFundsRequest request
    ) {
        return vaultService.addFunds(currentUser, vaultId, request);
    }

    @PostMapping("/{vaultId}/unlock")
    public VaultResponse unlock(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long vaultId
    ) {
        return vaultService.unlock(currentUser, vaultId);
    }

    @PostMapping("/{vaultId}/close")
    public VaultResponse closeEarly(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long vaultId
    ) {
        return vaultService.closeEarly(currentUser, vaultId);
    }
}
