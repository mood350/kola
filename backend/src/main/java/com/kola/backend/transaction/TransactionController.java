package com.kola.backend.transaction;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid DepositRequest request
    ) {
        return transactionService.deposit(currentUser, request);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid WithdrawalRequest request
    ) {
        return transactionService.withdraw(currentUser, request);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid TransferRequest request
    ) {
        return transactionService.transfer(currentUser, request);
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<TransactionResponse>> getWalletHistory(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long walletId
    ) {
        return ResponseEntity.ok(transactionService.getWalletHistory(currentUser, walletId));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponse> getByReference(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String reference
    ) {
        return ResponseEntity.ok(transactionService.getByReference(currentUser, reference));
    }
}
