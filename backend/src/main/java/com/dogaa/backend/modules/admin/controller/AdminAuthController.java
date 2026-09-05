package com.dogaa.backend.modules.admin.controller;

import com.dogaa.backend.modules.admin.dto.AdminIdentity;
import com.dogaa.backend.modules.admin.dto.AdminLoginRequest;
import com.dogaa.backend.modules.admin.dto.AdminLoginResponse;
import com.dogaa.backend.modules.admin.dto.ChangePasswordRequest;
import com.dogaa.backend.modules.admin.dto.PasswordResetRequest;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.admin.service.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Back-office sign-in (BACKEND.md 4.1).
 *
 * <p>These endpoints return the DTO directly, with no {@code ApiResponse} envelope: the admin
 * front-end maps the JSON one-to-one and would otherwise read the wrapper instead of the payload.
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin — authentification", description = "Connexion du back-office")
public class AdminAuthController {

    private final AdminAccountService adminAccountService;

    @PostMapping("/login")
    @Operation(summary = "Connexion par email et mot de passe")
    public AdminLoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminAccountService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Identité de l'administrateur connecté")
    public AdminIdentity me(@AuthenticationPrincipal CurrentAdmin admin) {
        return adminAccountService.me(admin.id());
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CurrentAdmin admin) {
        adminAccountService.logout(admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset-request")
    @Operation(summary = "Demande de réinitialisation — répond toujours 204")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        adminAccountService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changement de mot de passe")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CurrentAdmin admin,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        adminAccountService.changePassword(admin, request);
        return ResponseEntity.noContent().build();
    }
}
