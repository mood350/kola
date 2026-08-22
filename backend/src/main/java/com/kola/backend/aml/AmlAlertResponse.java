package com.kola.backend.aml;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vue analyste conformité d'une alerte. Contient volontairement l'identité du
 * client : c'est l'objet même du dispositif, et l'accès est restreint au rôle
 * ADMIN par SecurityConfig.
 */
public record AmlAlertResponse(
        Long id,
        Long userId,
        String userFullName,
        String userEmail,
        String transactionReference,
        BigDecimal transactionAmount,
        String transactionType,
        int riskScore,
        AmlRiskLevel riskLevel,
        AmlAlertStatus status,
        String triggeredRulesJson,
        String reviewNotes,
        String reviewedBy,
        LocalDateTime createdAt
) {
    public static AmlAlertResponse fromEntity(AmlAlert alert) {
        var tx = alert.getTransaction();
        var user = alert.getUser();
        return new AmlAlertResponse(
                alert.getId(),
                user != null ? user.getId() : null,
                user != null ? user.fullName() : null,
                user != null ? user.getEmail() : null,
                tx != null ? tx.getReference() : null,
                tx != null ? tx.getAmount() : null,
                tx != null && tx.getType() != null ? tx.getType().name() : null,
                alert.getRiskScore(),
                alert.getRiskLevel(),
                alert.getStatus(),
                alert.getTriggeredRulesJson(),
                alert.getReviewNotes(),
                alert.getReviewedBy(),
                alert.getCreatedAt()
        );
    }
}
