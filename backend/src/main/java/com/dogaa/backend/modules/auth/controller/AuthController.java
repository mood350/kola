package com.dogaa.backend.modules.auth.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.dto.AuthResponse;
import com.dogaa.backend.modules.auth.dto.ChangePinRequest;
import com.dogaa.backend.modules.auth.dto.LoginRequest;
import com.dogaa.backend.modules.auth.dto.RefreshTokenRequest;
import com.dogaa.backend.modules.auth.dto.RegisterRequest;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.auth.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration and phone + PIN login")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Create an account with a phone number and a PIN")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        AuthResponse response = authenticationService.register(request, userAgent(httpRequest), clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with a phone number and a PIN")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authenticationService.login(request, userAgent(httpRequest), clientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Logged in", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                             HttpServletRequest httpRequest) {
        AuthResponse response = authenticationService.refresh(request.refreshToken(),
                userAgent(httpRequest), clientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the presented refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authenticationService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke every refresh token of the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logoutEverywhere(@AuthenticationPrincipal CurrentUser currentUser) {
        authenticationService.logoutEverywhere(currentUser.id());
        return ResponseEntity.ok(ApiResponse.ok("Logged out from every device"));
    }

    @PostMapping("/change-pin")
    @Operation(summary = "Change the PIN; every existing session is revoked")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> changePin(@AuthenticationPrincipal CurrentUser currentUser,
                                                       @Valid @RequestBody ChangePinRequest request) {
        authenticationService.changePin(currentUser.id(), request);
        return ResponseEntity.ok(ApiResponse.ok("PIN changed, please log in again"));
    }

    private static String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }

    /** Behind a load balancer the real address is in X-Forwarded-For, first entry. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
