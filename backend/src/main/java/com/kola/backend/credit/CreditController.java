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
    private final RepaymentCapacityService repaymentCapacityService;
    private final LoanService loanService;

    // ── Score ──────────────────────────────────────────────────────

    /** Retourne le score actuel (recalculé si périmé). */
    @GetMapping("/score")
    public ResponseEntity<ScoreBreakdown> getMyScore(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(creditScoringService.getFresh(currentUser));
    }

    /**
     * Force le recalcul immédiat du score, sans attendre l'expiration du cache
     * (30 jours) ni le batch nocturne. Utilisé par le bouton "Recalculer" du
     * mobile : sans ça, un utilisateur qui vient d'effectuer des transactions
     * voit un score figé et croit que rien ne se met à jour.
     */
    @PostMapping("/score/refresh")
    public ResponseEntity<ScoreBreakdown> refreshMyScore(
            @AuthenticationPrincipal User currentUser) {
        creditScoringService.computeAndSave(currentUser.getId());
        return ResponseEntity.ok(creditScoringService.getFresh(currentUser));
    }

    /** Historique complet des scores. */
    /**
     * Capacité d'emprunt réelle, calculée sur les flux des 90 derniers jours.
     *
     * SÉPARÉE DE /score À DESSEIN. Le score dit la solvabilité — il fixe le
     * taux et un plafond de sécurité. Cette route dit le MONTANT, et deux
     * emprunteurs de même score obtiennent ici des valeurs différentes si leurs
     * entrées et sorties diffèrent. C'est cette route que l'écran de demande
     * doit lire pour borner le champ « montant », jamais maxLoanAmount du score.
     */
    @GetMapping("/capacity")
    public ResponseEntity<LoanCapacityResponse> getMyCapacity(
            @AuthenticationPrincipal User currentUser
    ) {
        ScoreBreakdown breakdown = creditScoringService.getFresh(currentUser);
        LoanCapacity capacity = repaymentCapacityService.compute(currentUser, breakdown.tier());
        return ResponseEntity.ok(LoanCapacityResponse.from(capacity, breakdown.tier()));
    }

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
