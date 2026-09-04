package com.dogaa.backend.modules.scoring.service;

import com.dogaa.backend.common.enums.KycTier;

import java.util.UUID;

/**
 * KYC tier lookup, owned by dogaa-kyc (developed by another team member on a
 * separate branch, not present on this branch yet). {@link DefaultKycStatusPort}
 * answers with the lowest tier so scoring can be built and tested in
 * isolation; swap the bean (or update the default impl) once dogaa-kyc is
 * merged into main.
 */
public interface KycStatusPort {

    KycTier getCurrentTier(UUID userId);
}
