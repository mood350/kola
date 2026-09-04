package com.dogaa.backend.modules.scoring;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.entity.CreditScore;
import com.dogaa.backend.modules.scoring.repository.CreditScoreRepository;
import com.dogaa.backend.modules.scoring.service.KycStatusPort;
import com.dogaa.backend.modules.scoring.service.ScoringServiceImpl;
import com.dogaa.backend.modules.scoring.service.TransactionSignalsPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScoringServiceImplTest {

    @Mock
    private CreditScoreRepository repository;

    @Mock
    private KycStatusPort kycStatusPort;

    @Mock
    private TransactionSignalsPort signalsPort;

    private ScoringServiceImpl scoringService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        scoringService = new ScoringServiceImpl(repository, kycStatusPort, signalsPort);
    }

    @Test
    void sumsTheFivePointCategoriesIntoTheOverallScore() {
        when(repository.save(any(CreditScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_1);
        when(signalsPort.depositRegularityPoints(userId)).thenReturn(15);
        when(signalsPort.savingsDisciplinePoints(userId)).thenReturn(10);
        when(signalsPort.transactionDiversityPoints(userId)).thenReturn(5);
        when(signalsPort.balanceStabilityPoints(userId)).thenReturn(20);
        when(signalsPort.scheduledReliabilityPoints(userId)).thenReturn(0);

        CreditScoreResponse response = scoringService.calculateScore(userId);

        assertThat(response.scoreValue()).isEqualTo(50);
        assertThat(response.userId()).isEqualTo(userId);
    }

    @Test
    void isNotCreditEligibleBelowTier2() {
        when(repository.save(any(CreditScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_1);

        assertThat(scoringService.calculateScore(userId).kycEligible()).isFalse();
    }

    @Test
    void isCreditEligibleFromTier2Upward() {
        when(repository.save(any(CreditScore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kycStatusPort.getCurrentTier(userId)).thenReturn(KycTier.TIER_2);

        assertThat(scoringService.calculateScore(userId).kycEligible()).isTrue();
    }

    @Test
    void getLatestScoreRaisesWhenNoScoreWasEverCalculated() {
        when(repository.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoringService.getLatestScore(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
