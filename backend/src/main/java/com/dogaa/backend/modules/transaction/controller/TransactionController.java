package com.dogaa.backend.modules.transaction.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.transaction.dto.CashOutRequest;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteRequest;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteResponse;
import com.dogaa.backend.modules.transaction.dto.MerchantPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.TransactionResponse;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.mapper.TransactionMapper;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transfers, merchant payments and cash-out (DOGAA.md 4.1)")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @GetMapping
    @Operation(summary = "My transaction history, newest first")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> history(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<TransactionResponse> history = transactionService.history(currentUser.id(), pageable)
                .map(transactionMapper::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get one transaction by reference")
    public ResponseEntity<ApiResponse<TransactionResponse>> get(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String reference) {
        return ResponseEntity.ok(ApiResponse.ok(transactionMapper.toResponse(
                transactionService.getForUser(currentUser.id(), reference))));
    }

    @PostMapping("/quote")
    @Operation(summary = "Preview the fee for an amount before sending")
    public ResponseEntity<ApiResponse<FeeQuoteResponse>> quote(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody FeeQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.quote(currentUser.id(), request)));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Send a P2P transfer (internal user or external number)")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.transfer(currentUser.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Transfer completed", tx));
    }

    @PostMapping("/merchant-payment")
    @Operation(summary = "Pay a partner merchant")
    public ResponseEntity<ApiResponse<TransactionResponse>> payMerchant(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody MerchantPaymentRequest request) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.payMerchant(currentUser.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Payment completed", tx));
    }

    @PostMapping("/cash-out")
    @Operation(summary = "Withdraw to an external Mobile Money account")
    public ResponseEntity<ApiResponse<TransactionResponse>> cashOut(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CashOutRequest request) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.cashOut(currentUser.id(), request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Cash-out completed", tx));
    }
}
