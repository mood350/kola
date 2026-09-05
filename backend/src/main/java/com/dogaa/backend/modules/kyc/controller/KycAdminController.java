package com.dogaa.backend.modules.kyc.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.security.ActorPrincipal;
import com.dogaa.backend.modules.kyc.dto.KycDocumentResponse;
import com.dogaa.backend.modules.kyc.dto.ReviewDocumentRequest;
import com.dogaa.backend.modules.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Back-office review queue (DOGAA.md 4.5).
 *
 * <p>{@code /api/v1/admin/**} is already reserved to ROLE_ADMIN by the filter chain; the
 * {@code @PreAuthorize} here is a second, method-level lock so a future change to the URL patterns
 * cannot silently open the queue.
 */
@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "KYC back-office", description = "Reviewing identity documents")
@SecurityRequirement(name = "bearerAuth")
public class KycAdminController {

    private final KycService kycService;

    @GetMapping("/documents/pending")
    @Operation(summary = "Documents waiting for review, oldest first")
    public ResponseEntity<ApiResponse<Page<KycDocumentResponse>>> pending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(kycService.listPendingDocuments(pageable)));
    }

    @GetMapping("/documents/{documentId}/file")
    @Operation(summary = "Download the file behind a document")
    public ResponseEntity<byte[]> download(@PathVariable UUID documentId) {
        KycService.StoredFile file = kycService.downloadDocument(documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                // inline would let a reviewer's browser render an attacker-supplied file in our origin.
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }

    @PostMapping("/documents/{documentId}/review")
    @Operation(summary = "Approve or reject a document; the tier is recomputed from the result")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> review(
            @AuthenticationPrincipal ActorPrincipal reviewer,
            @PathVariable UUID documentId,
            @Valid @RequestBody ReviewDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Review recorded",
                kycService.review(documentId, reviewer, request)));
    }

    @PostMapping("/documents/{documentId}/revoke")
    @Operation(summary = "Withdraw an approval; the user is demoted automatically")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> revoke(
            @AuthenticationPrincipal ActorPrincipal reviewer,
            @PathVariable UUID documentId,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Approval revoked",
                kycService.revokeApproval(documentId, reviewer, reason)));
    }
}
