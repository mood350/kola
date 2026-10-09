package com.kola.backend.modules.admin.controller;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.LoanStatus;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.admin.dto.RejectKycRequest;
import com.kola.backend.modules.admin.dto.console.ConsoleKycDocument;
import com.kola.backend.modules.admin.dto.console.ConsoleLoan;
import com.kola.backend.modules.admin.dto.console.ConsoleLoanSummary;
import com.kola.backend.modules.admin.dto.console.ConsoleOverview;
import com.kola.backend.modules.admin.dto.console.ConsolePage;
import com.kola.backend.modules.admin.dto.console.ConsoleTransaction;
import com.kola.backend.modules.admin.dto.console.ConsoleTransactionSummary;
import com.kola.backend.modules.admin.dto.console.ConsoleUserDetail;
import com.kola.backend.modules.admin.dto.console.ConsoleUserRow;
import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.modules.admin.service.AdminConsoleService.TransactionFilter;
import com.kola.backend.modules.admin.service.AdminConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The back-office console (see BACKEND.md §17). Periods are UTC calendar days — Lomé time —
 * and both bounds are inclusive.
 *
 * <p>Scoped to what the mobile app offers — accounts and KYC, wallets and vaults, transactions,
 * scheduled payments, credit — and serving raw values, never display strings. Access follows the
 * same role × module matrix as the rest of {@code /admin/**}: a module the role lacks is a 403.
 */
@RestController
@RequestMapping("/api/v1/admin/console")
@RequiredArgsConstructor
@Tag(name = "Admin — console", description = "Console d'administration (données brutes)")
@SecurityRequirement(name = "bearerAuth")
public class AdminConsoleController {

    private final AdminConsoleService consoleService;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('MODULE_DASHBOARD')")
    @Operation(summary = "Chiffres d'ouverture : utilisateurs, KYC en attente, volume 30 j, prêts")
    public ConsoleOverview overview() {
        return consoleService.overview();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Clients : recherche nom/téléphone, niveau KYC, pièce en attente, période d'inscription")
    public ConsolePage<ConsoleUserRow> users(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) KycTier kycTier,
                                             @RequestParam(defaultValue = "false") boolean kycPending,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "15") int size) {
        return consoleService.users(q, kycTier, kycPending, from, to, page, size);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Fiche client : comptes, coffres, prêts, paiements programmés, historique, pièces")
    public ConsoleUserDetail user(@PathVariable UUID id) {
        return consoleService.user(id);
    }

    @PostMapping("/users/{id}/unblock")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Lever une suspension ou un verrouillage PIN")
    public ConsoleUserDetail unblock(@AuthenticationPrincipal CurrentAdmin admin, @PathVariable UUID id) {
        return consoleService.unblock(admin, id);
    }

    @GetMapping("/kyc")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Pièces d'identité en attente, plus anciennes d'abord")
    public List<ConsoleKycDocument> kycQueue() {
        return consoleService.kycQueue();
    }

    @PostMapping("/kyc/{documentId}/approve")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Approuver une pièce — le niveau KYC est recalculé")
    public ResponseEntity<Void> approve(@AuthenticationPrincipal CurrentAdmin admin,
                                        @PathVariable UUID documentId) {
        consoleService.approveDocument(admin, documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/kyc/{documentId}/reject")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Rejeter une pièce — le motif est transmis au client")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal CurrentAdmin admin,
                                       @PathVariable UUID documentId,
                                       @Valid @RequestBody RejectKycRequest request) {
        consoleService.rejectDocument(admin, documentId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/loans")
    @PreAuthorize("hasAuthority('MODULE_CREDIT')")
    @Operation(summary = "Prêts, par statut et jour de versement (bornes incluses), plus récents d'abord")
    public ConsolePage<ConsoleLoan> loans(@RequestParam(required = false) LoanStatus status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "15") int size) {
        return consoleService.loans(status, from, to, page, size);
    }

    @GetMapping("/loans/summary")
    @PreAuthorize("hasAuthority('MODULE_CREDIT')")
    @Operation(summary = "Totaux des prêts filtrés : nombre, montant prêté, reste dû, en retard")
    public ConsoleLoanSummary loanSummary(@RequestParam(required = false) LoanStatus status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return consoleService.loanSummary(status, from, to);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Transactions : référence ou téléphone, client, type, statut, période (bornes incluses)")
    public ConsolePage<ConsoleTransaction> transactions(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) UUID userId,
                                                        @RequestParam(required = false) TransactionType type,
                                                        @RequestParam(required = false) TransactionStatus status,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "15") int size) {
        return consoleService.transactions(new TransactionFilter(q, userId, type, status, from, to), page, size);
    }

    @GetMapping("/transactions/summary")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Totaux comptables des transactions filtrées : nombre, volume, frais, par type")
    public ConsoleTransactionSummary transactionSummary(@RequestParam(required = false) String q,
                                                        @RequestParam(required = false) UUID userId,
                                                        @RequestParam(required = false) TransactionType type,
                                                        @RequestParam(required = false) TransactionStatus status,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return consoleService.transactionSummary(new TransactionFilter(q, userId, type, status, from, to));
    }

    /**
     * The filtered transactions as a file. {@code format=xlsx} adds a "Synthèse" sheet per type;
     * {@code csv} opens as-is in a French Excel. Capped at 50 000 lines — narrow the period beyond.
     */
    @GetMapping("/transactions/export")
    @PreAuthorize("hasAuthority('MODULE_USERS')")
    @Operation(summary = "Export CSV ou Excel des transactions filtrées")
    public ResponseEntity<byte[]> exportTransactions(@RequestParam(defaultValue = "csv") String format,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(required = false) UUID userId,
                                                     @RequestParam(required = false) TransactionType type,
                                                     @RequestParam(required = false) TransactionStatus status,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        boolean xlsx = "xlsx".equalsIgnoreCase(format);
        byte[] file = consoleService.exportTransactions(new TransactionFilter(q, userId, type, status, from, to), xlsx);

        String name = "transactions"
                + (from == null ? "" : "_" + from)
                + (to == null ? "" : "_" + to)
                + (xlsx ? ".xlsx" : ".csv");
        return ResponseEntity.ok()
                .contentType(xlsx
                        ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        : MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file);
    }
}
