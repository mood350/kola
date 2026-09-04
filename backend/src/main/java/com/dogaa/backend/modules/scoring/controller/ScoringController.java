package com.dogaa.backend.modules.scoring.controller;

import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.service.ScoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scoring")
public class ScoringController {

    private final ScoringService scoringService;

    public ScoringController(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    @PostMapping("/users/{userId}/calculate")
    public CreditScoreResponse calculate(@PathVariable Long userId) {
        return scoringService.calculateScore(userId);
    }

    @GetMapping("/users/{userId}")
    public CreditScoreResponse latest(@PathVariable Long userId) {
        return scoringService.getLatestScore(userId);
    }
}
