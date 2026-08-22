package com.kola.backend.scheduler;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Virements programmés (récurrents) de l'utilisateur courant.
 * Comme partout ailleurs, aucun ownerId n'est accepté en paramètre :
 * le propriétaire vient toujours du JWT.
 */
@RestController
@RequestMapping("/api/scheduled-transfers")
@RequiredArgsConstructor
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    @GetMapping
    public ResponseEntity<List<ScheduledTransferResponse>> getMine(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(scheduledTransferService.getMine(currentUser));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledTransferResponse create(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid CreateScheduledTransferRequest request
    ) {
        return scheduledTransferService.create(currentUser, request);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ScheduledTransferResponse> pause(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(scheduledTransferService.pause(currentUser, id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ScheduledTransferResponse> resume(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(scheduledTransferService.resume(currentUser, id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id
    ) {
        scheduledTransferService.delete(currentUser, id);
    }
}
