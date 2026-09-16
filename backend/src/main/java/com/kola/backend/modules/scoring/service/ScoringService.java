package com.kola.backend.modules.scoring.service;

import com.kola.backend.modules.scoring.dto.CreditScoreResponse;

import java.util.UUID;

public interface ScoringService {

    CreditScoreResponse calculateScore(UUID userId);

    CreditScoreResponse getLatestScore(UUID userId);
}
