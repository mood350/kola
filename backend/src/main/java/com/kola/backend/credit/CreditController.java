package com.kola.backend.credit;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CreditScoringService creditScoringService;
    private final LoanService loanService;

    // ── Score ──────────────────────────────────────────────────────

    /** Retourne le score actuel (recalculé si périmé). */
    @GetMapping("/score")
    public ResponseEntity<ScoreBreakdown> getMyScore(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(creditScoringService.getOrCompute(currentUser));
    }

    /** Historique complet des scores. */
    @GetMapping("/score/history")
    public ResponseEntity<List<ScoreBreakdown>> getScoreHistory(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(creditScoringService.getHistory(currentUser.getId()));
    }

    // ── Prêts ──────────────────────────────────────────────────────

    /** Dépose une demande de prêt. */
    @PostMapping("/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanDtos.LoanResponse applyForLoan(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid LoanDtos.LoanApplicationRequest request) {
        return loanService.apply(currentUser, request);
    }

    /** Liste tous les prêts de l'utilisateur. */
    @GetMapping("/loans")
    public ResponseEntity<List<LoanDtos.LoanResponse>> getMyLoans(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(loanService.getMyLoans(currentUser));
    }

    /** Détail d'un prêt. */
    @GetMapping("/loans/{loanId}")
    public ResponseEntity<LoanDtos.LoanResponse> getLoan(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.getLoan(currentUser, loanId));
    }

    /** Rembourse un prêt en une seule fois. */
    @PostMapping("/loans/{loanId}/repay")
    public ResponseEntity<LoanDtos.LoanResponse> repayLoan(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long loanId) {
        return ResponseEntity.ok(loanService.repay(currentUser, loanId));
    }
}
