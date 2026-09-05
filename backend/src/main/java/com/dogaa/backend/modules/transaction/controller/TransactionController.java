package com.dogaa.backend.modules.transaction.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.transaction.dto.BillPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.CashOutRequest;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteRequest;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteResponse;
import com.dogaa.backend.modules.transaction.dto.MerchantPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.TransactionResponse;
import com.dogaa.backend.modules.transaction.dto.RecipientLookupResponse;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.mapper.TransactionMapper;
import com.dogaa.backend.modules.transaction.service.TransactionEventBroadcaster;
import com.dogaa.backend.modules.transaction.service.RecipientDirectory;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

/**
 * Money movements.
 *
 * <p><b>Send an {@code Idempotency-Key} header on every one of these.</b> A phone on a weak
 * connection cannot tell a lost response from a refused payment, so it retries — and without a key
 * the retry is indistinguishable from a second, genuine payment. Same key, same UUID kept across
 * retries of the <em>same</em> user intent: the first call executes, later ones return that same
 * transaction. A new intent means a new key. The header is optional only so that existing clients
 * keep working; treat it as required.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transfers, merchant payments and cash-out (DOGAA.md 4.1)")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final RecipientDirectory recipientDirectory;
    private final TransactionEventBroadcaster transactionEventBroadcaster;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live feed of my transaction events (Server-Sent Events) — DOGAA.md 4.5 real-time tracking")
    public SseEmitter stream(@AuthenticationPrincipal CurrentUser currentUser) {
        return transactionEventBroadcaster.subscribe(currentUser.id());
    }

    @GetMapping
    @Operation(summary = "My transaction history, newest first — filterable by type, status and date range")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> history(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<TransactionResponse> history = transactionService
                .history(currentUser.id(), type, status, from, to, pageable)
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

    /**
     * Who a number belongs to, for the confirmation screen before a transfer.
     *
     * <p>A P2P transfer is irreversible without opening a dispute, so this is the last chance to
     * catch a mistyped digit. Send back the {@code phone} this returns rather than what the user
     * typed: it is normalised, so both calls address the same account.
     *
     * <p>An unknown number answers 200 with {@code registered: false}, not 404 — it is still
     * payable through Mobile Money, there is simply no name to confirm, and that is a different
     * thing from the lookup having failed.
     *
     * <p>Rate-limited per caller: a phone-to-name endpoint walked in a loop is a way to harvest the
     * names behind a numbering plan.
     */
    @GetMapping("/recipient")
    @Operation(summary = "Vérifier à qui appartient un numéro avant d'envoyer de l'argent")
    public ResponseEntity<ApiResponse<RecipientLookupResponse>> lookupRecipient(
            @AuthenticationPrincipal CurrentUser currentUser,
            @RequestParam String phone) {
        return ResponseEntity.ok(ApiResponse.ok(
                recipientDirectory.lookup(currentUser.id(), phone)));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Send a P2P transfer (internal user or external number)")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.executeIdempotent(idempotencyKey,
                        () -> transactionService.transfer(currentUser.id(), request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Transfer completed", tx));
    }

    @PostMapping("/merchant-payment")
    @Operation(summary = "Pay a partner merchant")
    public ResponseEntity<ApiResponse<TransactionResponse>> payMerchant(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody MerchantPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.executeIdempotent(idempotencyKey,
                        () -> transactionService.payMerchant(currentUser.id(), request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Payment completed", tx));
    }

    @PostMapping("/cash-out")
    @Operation(summary = "Withdraw to an external Mobile Money account")
    public ResponseEntity<ApiResponse<TransactionResponse>> cashOut(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CashOutRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.executeIdempotent(idempotencyKey,
                        () -> transactionService.cashOut(currentUser.id(), request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Cash-out completed", tx));
    }

    @PostMapping("/bill-payment")
    @Operation(summary = "Pay a utility bill (electricity, water, telecom, early loan repayment)")
    public ResponseEntity<ApiResponse<TransactionResponse>> payBill(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody BillPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse tx = transactionMapper.toResponse(
                transactionService.executeIdempotent(idempotencyKey,
                        () -> transactionService.payBill(currentUser.id(), request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Bill payment completed", tx));
    }
}
