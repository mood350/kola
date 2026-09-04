package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.scoring.dto.CreditScoreResponse;
import com.dogaa.backend.modules.scoring.entity.CreditScore;
import com.dogaa.backend.modules.scoring.mapper.CreditScoreMapper;
import com.dogaa.backend.modules.scoring.repository.CreditScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ScoringServiceImpl implements ScoringService {

    private final CreditScoreRepository repository;
    private final KycStatusPort kycStatusPort;
    private final TransactionSignalsPort signalsPort;

    public ScoringServiceImpl(CreditScoreRepository repository, KycStatusPort kycStatusPort, TransactionSignalsPort signalsPort) {
        this.repository = repository;
        this.kycStatusPort = kycStatusPort;
        this.signalsPort = signalsPort;
    }

    @Override
    public CreditScoreResponse calculateScore(Long userId) {
        KycTier tier = kycStatusPort.getCurrentTier(userId);

        CreditScore score = new CreditScore();
        score.setUserId(userId);
        score.setKycEligible(tier.ordinal() >= KycTier.TIER_2.ordinal());
        score.setDepositRegularityPoints(signalsPort.depositRegularityPoints(userId));
        score.setSavingsDisciplinePoints(signalsPort.savingsDisciplinePoints(userId));
        score.setTransactionDiversityPoints(signalsPort.transactionDiversityPoints(userId));
        score.setBalanceStabilityPoints(signalsPort.balanceStabilityPoints(userId));
        score.setScheduledReliabilityPoints(signalsPort.scheduledReliabilityPoints(userId));
        score.setScoreValue(
                score.getDepositRegularityPoints()
                        + score.getSavingsDisciplinePoints()
                        + score.getTransactionDiversityPoints()
                        + score.getBalanceStabilityPoints()
                        + score.getScheduledReliabilityPoints()
        );

        return CreditScoreMapper.toResponse(repository.save(score));
    }

    @Override
    @Transactional(readOnly = true)
    public CreditScoreResponse getLatestScore(Long userId) {
        return repository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(CreditScoreMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun score calcule pour l'utilisateur " + userId));
    }
}
