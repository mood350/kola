package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;

public interface ScoringService {

    CreditScoreResponse calculateScore(Long userId);

    CreditScoreResponse getLatestScore(Long userId);
}
