package com.kola.backend.modules.kyc.dto;

import com.kola.backend.common.enums.KycTier;

import java.util.List;

/** What the app needs to draw the verification screen in one call. */
public record KycStatusResponse(KycTier tier,
                                KycTier nextTier,
                                boolean phoneVerified,
                                boolean profileComplete,
                                boolean identityDocumentApproved,
                                List<String> requirementsForNextTier,
                                KycLimitsResponse limits,
                                List<KycDocumentResponse> documents) {
}
