package com.dogaa.backend.modules.scheduling.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import com.dogaa.backend.modules.scheduling.dto.BillerResponse;
import com.dogaa.backend.modules.transaction.service.BillerCatalog;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Programmed transactions (DOGAA.md 4.6).
 *
 * <p>Every route works on the caller from the token. The previous version took a {@code userId}
 * from the path and the request body, which let any signed-in user read and cancel someone else's
 * scheduled transfers — and schedule one out of their wallet.
 */
@RestController
@RequestMapping("/api/v1/scheduling/tasks")
@RequiredArgsConstructor
@Tag(name = "Transactions programmées", description = "Virements et paiements planifiés")
@SecurityRequirement(name = "bearerAuth")
public class ScheduledTaskController {

    private final ScheduledTaskService taskService;
    private final BillerCatalog billerCatalog;

    @PostMapping
    @Operation(summary = "Programmer une transaction pour soi-même")
    public ResponseEntity<ApiResponse<ScheduledTaskResponse>> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody ScheduledTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transaction programmée",
                        taskService.create(currentUser.id(), request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Ses propres transactions programmées")
    public ResponseEntity<ApiResponse<List<ScheduledTaskResponse>>> listMine(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.listByUser(currentUser.id())));
    }

    @PatchMapping("/{id}/pause")
    @Operation(summary = "Mettre en pause une de ses tâches")
    public ResponseEntity<ApiResponse<ScheduledTaskResponse>> pause(
            @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tâche en pause",
                taskService.pause(currentUser.id(), id)));
    }

    @PatchMapping("/{id}/resume")
    @Operation(summary = "Reprendre une de ses tâches")
    public ResponseEntity<ApiResponse<ScheduledTaskResponse>> resume(
            @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tâche reprise",
                taskService.resume(currentUser.id(), id)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Annuler une de ses tâches")
    public ResponseEntity<ApiResponse<ScheduledTaskResponse>> cancel(
            @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Tâche annulée",
                taskService.cancel(currentUser.id(), id)));
    }

    /**
     * The billers that can be put on a monthly schedule, with the identifier each one asks for.
     *
     * <p>The app must render the right label — "Numéro du compteur" for Cash Power, "Numéro de
     * carte" for Canal+ — instead of a generic "numéro", and must not offer a consumption bill for
     * a fixed monthly amount. Both come from here rather than from a list hard-coded in the app,
     * so adding a biller does not need a mobile release.
     */
    @GetMapping("/billers")
    @Operation(summary = "Services facturables programmables, et l'identifiant demandé par chacun")
    public ResponseEntity<ApiResponse<List<BillerResponse>>> billers() {
        List<BillerResponse> billers = billerCatalog.schedulable().stream()
                .map(BillerResponse::of)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(billers));
    }
}
