package com.kola.backend.admin;

import com.kola.backend.admin.AdminLoanDtos.AdminLoanOverview;
import com.kola.backend.admin.AdminLoanDtos.AdminLoanSummary;
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
 * pour l'ensemble du chemin. Lecture seule — voir `AdminLoanService` pour la
 * raison pour laquelle aucune mutation de statut n'est exposée ici.
 */
@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

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
