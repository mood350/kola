package com.kola.backend.modules.scoring.service;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.config.ScoringProperties;
import com.kola.backend.modules.scoring.dto.CreditScoreResponse;
import com.kola.backend.modules.scoring.dto.ScoreBreakdown;
import com.kola.backend.modules.scoring.entity.CreditScore;
import com.kola.backend.modules.scoring.mapper.CreditScoreMapper;
import com.kola.backend.modules.scoring.repository.CreditScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

    private final CreditScoreRepository repository;
    private final KycStatusPort kycStatusPort;
    private final ScoringDataCollector collector;
    private final ScoreCalculator calculator;
    private final ScoringProperties properties;

    @Override
    @Transactional
    public CreditScoreResponse calculateScore(UUID userId) {
        ScoreBreakdown breakdown = calculator.calculate(collector.collect(userId));
        KycTier tier = kycStatusPort.getCurrentTier(userId);

        CreditScore score = CreditScore.builder()
                .userId(userId)
                .rawScoreValue(breakdown.total())
                .scoreValue(smooth(userId, breakdown.total()))
                .kycTier(tier)
                .savingsDisciplinePoints(breakdown.savingsDiscipline())
                .financialStabilityPoints(breakdown.financialStability())
                .inflowRegularityPoints(breakdown.inflowRegularity())
                .usageIntensityPoints(breakdown.usageIntensity())
                .creditHistoryPoints(breakdown.creditHistory())
                .windowDays(properties.getWindowDays())
                .build();

        return CreditScoreMapper.toResponse(repository.save(score));
    }

    /**
     * Not {@code readOnly}: when no score exists yet this computes and stores the first one.
     *
     * <p>The read-only flag used to be here, and it was wrong twice over. Spring's proxy does not
     * intercept a call a bean makes to itself, so {@code calculateScore} inherited this method's
     * read-only transaction instead of opening its own — and PostgreSQL, unlike H2, refuses the
     * insert outright. The tests passed and production returned 500.
     */
    @Override
    @Transactional
    public CreditScoreResponse getLatestScore(UUID userId) {
        return latest(userId)
                .map(CreditScoreMapper::toResponse)
                .orElseGet(() -> calculateScore(userId));
    }

    /**
     * Blends today's raw score with yesterday's published one.
     *
     * <p>This is what makes the score expensive to game. A single evening of staged activity moves
     * the published value by at most 30% of the gap it opened; reaching a borrowing threshold takes
     * weeks of consistent behaviour, which is no longer gaming but the behaviour the product is
     * trying to reward. It also stops a quiet week from wiping out months of good conduct.
     */
    private int smooth(UUID userId, int rawScore) {
        return latest(userId)
                .map(previous -> {
                    double alpha = properties.getSmoothingFactor().doubleValue();
                    return (int) Math.round(alpha * rawScore + (1 - alpha) * previous.getScoreValue());
                })
                .orElse(rawScore);
    }

    private Optional<CreditScore> latest(UUID userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }
}
