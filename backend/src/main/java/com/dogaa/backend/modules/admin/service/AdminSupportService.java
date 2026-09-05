package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.util.RelativeTime;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.admin.dto.ManualActionResponse;
import com.dogaa.backend.modules.admin.dto.SupportTicketResponse;
import com.dogaa.backend.modules.admin.entity.SupportTicket;
import com.dogaa.backend.modules.admin.entity.SupportTicketStatus;
import com.dogaa.backend.modules.admin.repository.SupportTicketRepository;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.entity.AuditLogEntry;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer tickets and the trace of what agents did about them (BACKEND.md 13).
 *
 * <p>The "manual actions" feed is the audit journal filtered to this module rather than a second
 * table of its own. Two logs of the same events drift apart the moment one write is forgotten, and
 * the one an investigator trusts has to be the one everything writes to.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSupportService {

    private static final String MODULE = "support";

    private final SupportTicketRepository ticketRepository;
    private final UserService userService;
    private final AuditService auditService;

    // --- reading ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> tickets() {
        return ticketRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /** What agents have done lately, newest first — the audit trail, narrowed to support. */
    @Transactional(readOnly = true)
    public List<ManualActionResponse> manualActions() {
        return auditService.search(null, MODULE, null, null).stream()
                .map(AdminSupportService::toManualAction)
                .toList();
    }

    // --- actions ----------------------------------------------------------

    @Transactional
    public SupportTicketResponse takeCharge(CurrentAdmin admin, String reference) {
        SupportTicket ticket = require(reference);

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new ConflictException("Ce ticket est déjà pris en charge");
        }

        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        ticket.setAssignedAdminId(admin.id());
        ticketRepository.save(ticket);

        auditService.record(admin, MODULE, "Ticket " + reference + " pris en charge",
                AuditService.diff("open", "in_progress"), "SupportTicket", reference);

        log.info("Admin {} took charge of ticket {}", admin.email(), reference);
        return toResponse(ticket);
    }

    /**
     * Marks the ticket resolved. Reachable straight from {@code open}: an agent who answers a
     * question on the spot should not have to claim the ticket first just to close it.
     */
    @Transactional
    public SupportTicketResponse resolve(CurrentAdmin admin, String reference) {
        SupportTicket ticket = require(reference);

        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            throw new ConflictException("Ce ticket est déjà résolu");
        }

        SupportTicketStatus previous = ticket.getStatus();
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        if (ticket.getAssignedAdminId() == null) {
            ticket.setAssignedAdminId(admin.id());
        }
        ticketRepository.save(ticket);

        auditService.record(admin, MODULE, "Ticket " + reference + " marqué résolu",
                AuditService.diff(previous.wireValue(), "resolved"), "SupportTicket", reference);

        log.info("Admin {} resolved ticket {}", admin.email(), reference);
        return toResponse(ticket);
    }

    // --- mapping ----------------------------------------------------------

    private SupportTicket require(String reference) {
        return ticketRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + reference));
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        return new SupportTicketResponse(
                ticket.getReference(),
                ticket.getSubject(),
                userService.getById(ticket.getUserId()).getFullName(),
                ticket.getStatus().wireValue());
    }

    private static ManualActionResponse toManualAction(AuditLogEntry entry) {
        return new ManualActionResponse(
                entry.getId().toString(),
                entry.getAction(),
                entry.getActorName(),
                RelativeTime.since(entry.getCreatedAt()));
    }
}
