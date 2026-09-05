package com.dogaa.backend.modules.wallet.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.wallet.dto.CreateWalletRequest;
import com.dogaa.backend.modules.wallet.dto.DepositRequest;
import com.dogaa.backend.modules.wallet.dto.SavingsMovementRequest;
import com.dogaa.backend.modules.wallet.dto.WalletResponse;
import com.dogaa.backend.modules.wallet.mapper.WalletMapper;
import com.dogaa.backend.modules.wallet.service.WalletService;
import com.dogaa.backend.modules.transaction.service.TransactionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Multi-currency balances of the authenticated user (DOGAA.md 4.1)")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List my wallets")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> list(
            @AuthenticationPrincipal CurrentUser currentUser) {
        List<WalletResponse> wallets = walletService.listWallets(currentUser.id())
                .stream().map(walletMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(wallets));
    }

    @PostMapping
    @Operation(summary = "Open a wallet in a new currency")
    public ResponseEntity<ApiResponse<WalletResponse>> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse wallet = walletMapper.toResponse(
                walletService.createWallet(currentUser.id(), request.currency()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Wallet created", wallet));
    }

    @GetMapping("/{currency}")
    @Operation(summary = "Get one wallet by currency")
    public ResponseEntity<ApiResponse<WalletResponse>> get(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Currency currency) {
        return ResponseEntity.ok(ApiResponse.ok(
                walletMapper.toResponse(walletService.getWallet(currentUser.id(), currency))));
    }

    @PostMapping("/{currency}/deposit")
    @Operation(summary = "Cash-in (free) — credit the available balance")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable Currency currency,
            @Valid @RequestBody DepositRequest request) {
        // Routed through TransactionService (not WalletService.deposit directly) so the
        // cash-in leaves a CASH_IN trace in the transaction history, same as every other
        // money movement.
        transactionService.cashIn(currentUser.id(), currency, request.amount());
        WalletResponse wallet = walletMapper.toResponse(walletService.getWallet(currentUser.id(), currency));
        return ResponseEntity.ok(ApiResponse.ok("Deposit completed", wallet));
    }

    // --- Savings account --------------------------------------------------

    /**
     * Funds the savings account that secures loans.
     *
     * <p>The savings wallet is created at sign-up but had no way in until this route existed, so
     * no borrower could ever meet the minimum collateral and the savings axis of the score stayed
     * at zero.
     */
    @PostMapping("/savings/deposit")
    @Operation(summary = "Verser du compte courant vers le compte épargne — gratuit")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> depositToSavings(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SavingsMovementRequest request) {
        transactionService.depositToSavings(currentUser.id(), request.currency(), request.amount());
        return ResponseEntity.ok(ApiResponse.ok("Versement effectué", myWallets(currentUser)));
    }

    /**
     * Takes money back out of savings.
     *
     * <p>A running loan freezes the collateral into the locked balance, and a debit only ever
     * spends the available side — so this is refused for as long as the loan runs, without a rule
     * of its own.
     */
    @PostMapping("/savings/withdraw")
    @Operation(summary = "Retirer du compte épargne vers le compte courant — gratuit")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> withdrawFromSavings(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody SavingsMovementRequest request) {
        transactionService.withdrawFromSavings(currentUser.id(), request.currency(), request.amount());
        return ResponseEntity.ok(ApiResponse.ok("Retrait effectué", myWallets(currentUser)));
    }

    /** Both accounts after the move: the client needs the pair, not one side of it. */
    private List<WalletResponse> myWallets(CurrentUser currentUser) {
        return walletService.listWallets(currentUser.id())
                .stream().map(walletMapper::toResponse).toList();
    }
}
