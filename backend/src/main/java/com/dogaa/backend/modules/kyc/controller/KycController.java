package com.dogaa.backend.modules.kyc.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.kyc.dto.KycDocumentResponse;
import com.dogaa.backend.modules.kyc.dto.KycStatusResponse;
import com.dogaa.backend.modules.kyc.dto.VerifyEmailRequest;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Progressive verification and transaction limits")
@SecurityRequirement(name = "bearerAuth")
public class KycController {

    private final KycService kycService;

    @GetMapping("/status")
    @Operation(summary = "Current tier, limits, submitted documents and what the next tier needs")
    public ResponseEntity<ApiResponse<KycStatusResponse>> status(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(kycService.getStatus(currentUser.id())));
    }

    @PostMapping("/email/request-code")
    @Operation(summary = "Tier 1: send a code to the email address on the profile")
    public ResponseEntity<ApiResponse<Map<String, Instant>>> requestEmailCode(
            @AuthenticationPrincipal CurrentUser currentUser) {
        Instant resendAvailableAt = kycService.requestEmailVerification(currentUser.id());
        return ResponseEntity.ok(ApiResponse.ok("Code sent by email",
                Map.of("resendAvailableAt", resendAvailableAt)));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Tier 1: confirm the email address with the code")
    public ResponseEntity<ApiResponse<KycStatusResponse>> verifyEmail(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Email verified",
                kycService.confirmEmailVerification(currentUser.id(), request.code())));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tiers 2 and 3: upload a document for review")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> submitDocument(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam KycDocumentType type,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Document submitted for review",
                        kycService.submitDocument(currentUser.id(), type, file)));
    }

    @GetMapping("/documents")
    @Operation(summary = "Documents submitted by the authenticated user")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> listDocuments(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(kycService.listDocuments(currentUser.id())));
    }
}
