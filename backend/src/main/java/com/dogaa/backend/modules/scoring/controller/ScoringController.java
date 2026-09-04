package com.dogaa.backend.modules.scoring.controller;

import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.service.ScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scoring")
@Tag(name = "Scoring", description = "Alternative credit score (DOGAA.md 3.2)")
@SecurityRequirement(name = "bearerAuth")
public class ScoringController {

    private final ScoringService scoringService;

    public ScoringController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping("/users/{userId}/calculate")
    @Operation(summary = "Recompute the 0-100 score from KYC tier and behavioral signals")
    public CreditScoreResponse calculate(@PathVariable UUID userId) {
        return scoringService.calculateScore(userId);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get the most recently calculated score")
    public CreditScoreResponse latest(@PathVariable UUID userId) {
        return scoringService.getLatestScore(userId);
    }
}
