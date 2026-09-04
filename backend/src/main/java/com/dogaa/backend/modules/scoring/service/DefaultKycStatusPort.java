package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.common.enums.KycTier;
import org.springframework.stereotype.Service;

/**
 * Placeholder implementation: always reports TIER_0 (not credit-eligible).
 * Replace by wiring the real dogaa-kyc service once it is merged into main
 * (see {@link KycStatusPort}).
 */
@Service
public class DefaultKycStatusPort implements KycStatusPort {

    @Override
    public KycTier getCurrentTier(Long userId) {
        return KycTier.TIER_0;
    }
}
