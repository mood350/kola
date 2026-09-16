package com.kola.backend.modules.credit.mapper;

import com.kola.backend.modules.credit.dto.LoanResponse;
import com.kola.backend.modules.credit.entity.Loan;
import org.springframework.stereotype.Component;

@Component
public class LoanMapper {

    public LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getCurrency(),
                loan.getPrincipal(),
                loan.getCollateralAmount(),
                loan.getLeverageRatio(),
                loan.getMonthlyRatePercent(),
                loan.getInterestAmount(),
                loan.getPenaltyAmount(),
                loan.getTotalDue(),
                loan.getAmountRepaid(),
                loan.getOutstanding(),
                loan.getShortfallAmount(),
                loan.getStatus(),
                loan.getScoreAtGrant(),
                loan.getDisbursedAt(),
                loan.getDueAt(),
                loan.getSettledAt());
    }
}
