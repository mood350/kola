package com.kola.backend.transaction;

import com.kola.backend.payment.MobileMoneyDepositService;
import com.kola.backend.payment.MobileMoneyWithdrawalRequest;
import com.kola.backend.payment.MobileMoneyWithdrawalService;
import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final MobileMoneyDepositService mobileMoneyDepositService;
    private final MobileMoneyWithdrawalService mobileMoneyWithdrawalService;

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid DepositRequest request
    ) {
        return transactionService.deposit(currentUser, request);
    }

    /**
     * Rechargement depuis un compte Mobile Money, via le prestataire.
     *
     * 202 ACCEPTED et non 201 : rien n'est encore acquis. La demande part sur
     * le téléphone du client, qui doit la valider ; le crédit n'aura lieu qu'à
     * la notification du prestataire. Répondre 201 laisserait croire à un dépôt
     * abouti et ferait afficher un solde qui n'a pas bougé.
     *
     * À NE PAS CONFONDRE avec POST /deposit, qui crédite immédiatement et
     * n'existe que pour le développement et les corrections manuelles.
     */
    @PostMapping("/deposit/mobile-money")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse depositByMobileMoney(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid MobileMoneyDepositRequest request
    ) {
        return mobileMoneyDepositService.deposit(currentUser, request);
    }

    /**
     * Retrait réel vers un compte Mobile Money, via le prestataire.
     *
     * 202 ACCEPTED : le portefeuille est débité immédiatement — sans quoi la
     * somme resterait dépensable pendant le traitement — mais l'argent n'est
     * pas encore arrivé chez l'opérateur. L'écran doit dire « retrait en
     * cours ». En cas d'échec, montant et frais sont recrédités.
     *
     * À NE PAS CONFONDRE avec POST /withdraw, qui débite sans rien envoyer
     * nulle part et n'existe que pour le développement.
     */
    @PostMapping("/withdraw/mobile-money")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse withdrawByMobileMoney(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid MobileMoneyWithdrawalRequest request
    ) {
        return mobileMoneyWithdrawalService.withdraw(currentUser, request);
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

    @PostMapping("/pay-merchant")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse payMerchant(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid PayMerchantRequest request
    ) {
        return transactionService.payMerchant(currentUser, request);
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<Page<TransactionResponse>> getWalletHistory(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long walletId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(transactionService.getWalletHistory(currentUser, walletId, pageable));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponse> getByReference(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String reference
    ) {
        return ResponseEntity.ok(transactionService.getByReference(currentUser, reference));
    }
}
