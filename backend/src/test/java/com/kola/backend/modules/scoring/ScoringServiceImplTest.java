package com.kola.backend.modules.scoring;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.config.ScoringProperties;
import com.kola.backend.modules.scoring.dto.CreditScoreResponse;
import com.kola.backend.modules.scoring.entity.CreditScore;
import com.kola.backend.modules.scoring.repository.CreditScoreRepository;
import com.kola.backend.modules.scoring.service.KycStatusPort;
import com.kola.backend.modules.scoring.service.ScoreCalculator;
import com.kola.backend.modules.scoring.service.ScoringDataCollector;
import com.kola.backend.modules.scoring.service.ScoringInputs;
import com.kola.backend.modules.scoring.service.ScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceImplTest {

    @Mock
    private CreditScoreRepository repository;

    @Mock
    private KycStatusPort kycStatusPort;

    @Mock
    private ScoringDataCollector collector;

    private final ScoringProperties properties = new ScoringProperties();

    private ScoringServiceImpl scoringService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        scoringService = new ScoringServiceImpl(
                repository, kycStatusPort, collector, new ScoreCalculator(properties), properties);
    }

    private static ScoringInputs strongInputs() {
        return new ScoringInputs(
                new BigDecimal("400000"), new BigDecimal("260000"),
                new BigDecimal("80000"), new BigDecimal("80000"), BigDecimal.ZERO,
                4, 0,
                new BigDecimal("120000"), new BigDecimal("12000"), new BigDecimal("8600"), 0,
                10, 0.15, ScoringInputs.Trend.GROWING,
                3, 7,
                3, 0, 0);
    }

    @Test
    void storesTheAxisBreakdownAlongsideTheTotal() {
        when(repository.save(any(CreditScore.class))).thenAnswer(call -> call.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_2);
        when(collector.collect(userId)).thenReturn(strongInputs());
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        CreditScoreResponse response = scoringService.calculateScore(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.kycTier()).isEqualTo(KycTier.TIER_2);
        assertThat(response.breakdown().total()).isEqualTo(response.rawScoreValue());
        assertThat(response.breakdown().savingsDiscipline()
                + response.breakdown().financialStability()
                + response.breakdown().inflowRegularity()
                + response.breakdown().usageIntensity()
                + response.breakdown().creditHistory())
                .isEqualTo(response.rawScoreValue());
    }

    @Test
    void theFirstEverScoreIsPublishedRaw() {
        when(repository.save(any(CreditScore.class))).thenAnswer(call -> call.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_2);
        when(collector.collect(userId)).thenReturn(strongInputs());
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        CreditScoreResponse response = scoringService.calculateScore(userId);

        assertThat(response.scoreValue()).isEqualTo(response.rawScoreValue());
    }

    /**
     * The smoothing is what makes the score expensive to game: a perfect day on top of a poor
     * history lands well short of the raw value, so reaching a lending threshold takes weeks of
     * consistent behaviour rather than one staged evening.
     */
    @Test
    void oneGoodDayOnlyMovesThePublishedScorePartOfTheWay() {
        CreditScore yesterday = CreditScore.builder().userId(userId).scoreValue(20).build();
        when(repository.save(any(CreditScore.class))).thenAnswer(call -> call.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_2);
        when(collector.collect(userId)).thenReturn(strongInputs());
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(yesterday));

        CreditScoreResponse response = scoringService.calculateScore(userId);

        int expected = (int) Math.round(0.7 * response.rawScoreValue() + 0.3 * 20);
        assertThat(response.scoreValue()).isEqualTo(expected);
        assertThat(response.scoreValue()).isLessThan(response.rawScoreValue());
    }

    @Test
    void aQuietWeekDoesNotWipeOutMonthsOfGoodConduct() {
        CreditScore yesterday = CreditScore.builder().userId(userId).scoreValue(90).build();
        when(repository.save(any(CreditScore.class))).thenAnswer(call -> call.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_2);
        when(collector.collect(userId)).thenReturn(ScoringInputs.empty());
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(yesterday));

        CreditScoreResponse response = scoringService.calculateScore(userId);

        assertThat(response.rawScoreValue()).isEqualTo(5);
        assertThat(response.scoreValue()).isGreaterThan(response.rawScoreValue());
    }

    @Test
    void askingForAScoreThatWasNeverComputedCalculatesOneRatherThanFailing() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());
        when(repository.save(any(CreditScore.class))).thenAnswer(call -> call.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_0);
        when(collector.collect(userId)).thenReturn(ScoringInputs.empty());

        assertThat(scoringService.getLatestScore(userId).userId()).isEqualTo(userId);
    }
}
