package com.kola.backend.admin;

import com.kola.backend.admin.AdminLoanDtos.AdminLoanOverview;
import com.kola.backend.admin.AdminLoanDtos.AdminLoanSummary;
import com.kola.backend.credit.LoanDtos;
import com.kola.backend.credit.LoanService;
import com.kola.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.kola.backend.credit.LoanStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portefeuille de prêts, vue administration.
 *
 * Monté sous `/api/admin/**` : l'autorité ADMIN est exigée par `SecurityConfig`
 * pour l'ensemble du chemin.
 *
 * ═══ LECTURE SEULE, SAUF LA DÉCISION D'EXAMEN ═══
 *
 * L'invariant du module tient toujours : AUCUN endpoint n'écrit ici le statut
 * d'un prêt. Les deux routes de décision ci-dessous délèguent à `LoanService`,
 * qui déplace l'argent, écrit au grand livre et notifie l'emprunteur dans la
 * même transaction. Un `setStatus(APPROVED)` posé depuis un service
 * d'administration aurait désynchronisé le statut affiché et le solde réel —
 * c'est précisément ce que l'invariant interdit, et ce que cette délégation
 * respecte.
 *
 * Ces routes n'existent que parce qu'au-delà de
 * {@link com.kola.backend.credit.LoanService#manualReviewThreshold()} un prêt
 * n'est plus accordé automatiquement.
 */
@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
public class AdminLoanController {

    private final AdminLoanService adminLoanService;
    private final LoanService loanService;

    /**
     * Accorde un prêt en attente d'examen, et le débourse.
     *
     * L'argent part immédiatement après cet appel : c'est une action
     * irréversible, et la console doit la faire confirmer.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanDtos.LoanResponse> approve(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(loanService.approve(admin, id));
    }

    /**
     * Refuse un prêt en attente d'examen.
     *
     * Le motif est obligatoire (10 à 500 caractères, comme pour les décisions
     * KYC) : il est transmis tel quel à l'emprunteur, et c'est la seule trace
     * exploitable si la décision est contestée.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanDtos.LoanResponse> reject(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @RequestBody @Valid AdminLoanDtos.RejectLoanRequest request
    ) {
        return ResponseEntity.ok(loanService.reject(admin, id, request.reason()));
    }

    /**
     * Liste paginée, filtrable par statut.
     *
     * Tri par défaut sur la date de création décroissante : les dossiers
     * récents sont ceux sur lesquels une décision reste à prendre.
     */
    @GetMapping
    public ResponseEntity<Page<AdminLoanSummary>> list(
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminLoanService.list(status, pageable));
    }

    /**
     * Volumétrie du portefeuille.
     *
     * Déclaré AVANT `/{id}` : Spring retiendrait sinon le chemin variable pour
     * « /overview » et tenterait de convertir la chaîne en identifiant. Le
     * mapping le plus spécifique doit précéder le plus général.
     */
    @GetMapping("/overview")
    public ResponseEntity<AdminLoanOverview> overview() {
        return ResponseEntity.ok(adminLoanService.overview());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminLoanSummary> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminLoanService.detail(id));
    }
}
