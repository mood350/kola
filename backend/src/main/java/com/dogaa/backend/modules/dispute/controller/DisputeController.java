package com.dogaa.backend.modules.dispute.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.dispute.dto.DisputeResponse;
import com.dogaa.backend.modules.dispute.dto.OpenDisputeRequest;
import com.dogaa.backend.modules.dispute.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where disputes come from: a customer contesting one of their own transactions.
 *
 * <p>Customer-facing, so this one uses the usual {@code ApiResponse} envelope.
 */
@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
@Tag(name = "Litiges", description = "Contester une transaction")
@SecurityRequirement(name = "bearerAuth")
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    @Operation(summary = "Contester une de ses transactions")
    public ResponseEntity<ApiResponse<DisputeResponse>> open(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody OpenDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Litige enregistré",
                        disputeService.open(currentUser.id(), request)));
    }
}
