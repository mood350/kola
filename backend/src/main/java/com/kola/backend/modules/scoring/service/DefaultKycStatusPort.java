package com.kola.backend.modules.scoring.service;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.modules.kyc.service.KycService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Wires the real kola-kyc service (owned by another team member, merged into
 * main) now that it exists. Calling KycService directly is a same-module-owner
 * exception to the port pattern used for wallet/transaction, which are not
 * merged yet.
 */
@Service
public class DefaultKycStatusPort implements KycStatusPort {

    private final KycService kycService;

    public DefaultKycStatusPort(KycService kycService) {
        this.kycService = kycService;
    }

    @Override
    public KycTier getCurrentTier(UUID userId) {
        return kycService.getStatus(userId).tier();
    }
}
