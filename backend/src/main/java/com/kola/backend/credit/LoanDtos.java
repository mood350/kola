package com.kola.backend.credit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class LoanDtos {

    private LoanDtos() {}

    public record LoanApplicationRequest(
            @NotNull(message = "Le wallet de destination est obligatoire")
            Long walletId,

            @NotNull(message = "Le montant demandé est obligatoire")
            @DecimalMin(value = "1000", message = "Montant minimum : 1 000 XOF")
            BigDecimal requestedAmount,

            @NotNull(message = "La durée est obligatoire")
            @Min(value = 1, message = "Durée minimum : 1 mois")
            @Max(value = 12, message = "Durée maximum : 12 mois")
            Integer durationMonths,

            String purpose
    ) {}

    public record LoanResponse(
            Long id,
            BigDecimal requestedAmount,
            BigDecimal totalRepayment,
            BigDecimal monthlyRate,
            int durationMonths,
            LoanStatus status,
            String purpose,
            LocalDate dueDate,
            String rejectionReason,
            int creditScoreAtRequest,
            CreditTier tierAtRequest
    ) {
        public static LoanResponse fromEntity(LoanRequest loan) {
            return new LoanResponse(
                    loan.getId(),
                    loan.getRequestedAmount(),
                    loan.getTotalRepayment(),
                    loan.getMonthlyRate(),
                    loan.getDurationMonths(),
                    loan.getStatus(),
                    loan.getPurpose(),
                    loan.getDueDate(),
                    loan.getRejectionReason(),
                    loan.getCreditScoreSnapshot().getScore(),
                    loan.getCreditScoreSnapshot().getTier()
            );
        }
    }
}
