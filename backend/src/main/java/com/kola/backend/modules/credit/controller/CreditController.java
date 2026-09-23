package com.kola.backend.modules.credit.controller;

import com.kola.backend.common.dto.ApiResponse;
import com.kola.backend.common.enums.Currency;
import com.kola.backend.modules.auth.security.CurrentUser;
import com.kola.backend.modules.credit.dto.CreditEligibilityResponse;
import com.kola.backend.modules.credit.dto.LoanRequest;
import com.kola.backend.modules.credit.dto.LoanResponse;
import com.kola.backend.modules.credit.dto.RepaymentRequest;
import com.kola.backend.modules.credit.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credit")
@RequiredArgsConstructor
@Tag(name = "Credit", description = "Loans secured against the savings account (KOLA.md 4.3)")
@SecurityRequirement(name = "bearerAuth")
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/eligibility")
    @Operation(summary = "Score, borrowing ceiling and rate — or exactly what is blocking them")
    public ResponseEntity<ApiResponse<CreditEligibilityResponse>> eligibility(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "XOF") Currency currency) {
        return ResponseEntity.ok(ApiResponse.ok(
                creditService.checkEligibility(currentUser.id(), currency)));
    }

    @PostMapping("/loans")
    @Operation(summary = "Draw a loan; the savings account is frozen until it is repaid")
    public ResponseEntity<ApiResponse<LoanResponse>> requestLoan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Loan granted and paid to your current account",
                        creditService.requestLoan(currentUser.id(), request)));
    }

    @GetMapping("/loans")
    @Operation(summary = "Own loans, newest first")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> listLoans(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(creditService.listLoans(currentUser.id())));
    }

    @PostMapping("/loans/{loanId}/repay")
    @Operation(summary = "Repay all or part of a loan; full repayment releases the collateral")
    public ResponseEntity<ApiResponse<LoanResponse>> repay(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID loanId,
            @Valid @RequestBody(required = false) RepaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Repayment recorded",
                creditService.repay(currentUser.id(), loanId,
                        request == null ? null : request.amount())));
    }
}
