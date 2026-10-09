package com.kola.backend.modules.scoring.controller;

import com.kola.backend.common.dto.ApiResponse;
import com.kola.backend.modules.auth.security.CurrentUser;
import com.kola.backend.modules.scoring.dto.CreditScoreResponse;
import com.kola.backend.modules.scoring.service.ScoringService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scoring")
@RequiredArgsConstructor
@Tag(name = "Scoring", description = "Alternative credit score (KOLA.md 3.2)")
@SecurityRequirement(name = "bearerAuth")
public class ScoringController {

    private final ScoringService scoringService;

    @GetMapping("/me")
    @Operation(summary = "Own score, with the five axes broken out")
    public ResponseEntity<ApiResponse<CreditScoreResponse>> myScore(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(scoringService.getLatestScore(currentUser.id())));
    }

    @PostMapping("/me/recalculate")
    @Operation(summary = "Recompute own score now instead of waiting for the nightly pass")
    public ResponseEntity<ApiResponse<CreditScoreResponse>> recalculateMyScore(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok("Score recalculated",
                scoringService.calculateScore(currentUser.id())));
    }

    /** A score is personal data: reading someone else's is an administrator's job, not a user's. */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Read another user's score")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditScoreResponse>> userScore(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(scoringService.getLatestScore(userId)));
    }
}
