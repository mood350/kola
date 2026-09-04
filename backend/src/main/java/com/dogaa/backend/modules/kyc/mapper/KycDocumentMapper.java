package com.dogaa.backend.modules.kyc.mapper;

import com.dogaa.backend.config.KycProperties;
import com.dogaa.backend.modules.kyc.dto.KycDocumentResponse;
import com.dogaa.backend.modules.kyc.dto.KycLimitsResponse;
import com.dogaa.backend.modules.kyc.entity.KycDocument;
import org.springframework.stereotype.Component;

@Component
public class KycDocumentMapper {

    public KycDocumentResponse toResponse(KycDocument document) {
        return new KycDocumentResponse(
                document.getId(),
                document.getType(),
                document.getStatus(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getRejectionReason(),
                document.getReviewedAt(),
                document.getCreatedAt());
    }

    public KycLimitsResponse toResponse(KycProperties.TierLimits limits) {
        return new KycLimitsResponse(
                limits.getPerTransaction(),
                limits.getDaily(),
                limits.getMonthly(),
                limits.getBalanceCap(),
                limits.isCreditEligible());
    }
}
