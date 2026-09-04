package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;

import java.util.UUID;

public interface ScoringService {

    CreditScoreResponse calculateScore(UUID userId);

    CreditScoreResponse getLatestScore(UUID userId);
}
