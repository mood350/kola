package com.dogaa.backend.modules.auth.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.dto.AuthResponse;
import com.dogaa.backend.modules.auth.dto.ChangePinRequest;
import com.dogaa.backend.modules.auth.dto.LoginRequest;
import com.dogaa.backend.modules.auth.dto.OtpRequestedResponse;
import com.dogaa.backend.modules.auth.dto.OtpVerifiedResponse;
import com.dogaa.backend.modules.auth.dto.RequestOtpRequest;
import com.dogaa.backend.modules.auth.dto.VerifyOtpRequest;
import com.dogaa.backend.modules.auth.dto.RefreshTokenRequest;
import com.dogaa.backend.modules.auth.dto.RegisterRequest;
import com.dogaa.backend.modules.auth.dto.VerifyEmailRequest;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.auth.service.AuthenticationService;
import com.dogaa.backend.modules.auth.service.EmailVerificationService;
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

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration and phone + PIN login")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register/request-otp")
    @Operation(summary = "Step 1: send a one-time code to a phone number")
    public ResponseEntity<ApiResponse<OtpRequestedResponse>> requestRegistrationOtp(
            @Valid @RequestBody RequestOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Code sent by SMS",
                authenticationService.requestRegistrationOtp(request)));
    }

    @PostMapping("/register/verify-otp")
    @Operation(summary = "Step 2: check the code and obtain the verification token")
    public ResponseEntity<ApiResponse<OtpVerifiedResponse>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Phone number verified",
                authenticationService.verifyRegistrationOtp(request)));
    }

    @PostMapping("/register")
    @Operation(summary = "Step 3: set the identity and the PIN, using the verification token")
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

    @PostMapping("/email/request-code")
    @Operation(summary = "Send a code to the email address on the profile. Optional: "
            + "a verified email unlocks receipts and notifications, not a KYC tier")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, Instant>>> requestEmailCode(
            @AuthenticationPrincipal CurrentUser currentUser) {
        Instant resendAvailableAt = emailVerificationService.requestCode(currentUser.id());
        return ResponseEntity.ok(ApiResponse.ok("Code sent by email",
                Map.of("resendAvailableAt", resendAvailableAt)));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Confirm the email address with the code")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.confirm(currentUser.id(), request.code());
        return ResponseEntity.ok(ApiResponse.ok("Email verified"));
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
