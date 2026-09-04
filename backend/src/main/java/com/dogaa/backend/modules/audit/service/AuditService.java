package com.dogaa.backend.modules.audit.service;

import com.dogaa.backend.modules.audit.entity.AuditLogEntry;
import com.dogaa.backend.modules.audit.repository.AuditLogRepository;
import com.dogaa.backend.modules.auth.security.ActorPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The one place sensitive actions are recorded.
 *
 * <p>Every module writes here; this module reads nothing back from them. That direction is what
 * keeps the audit trail free of cycles as the back-office grows.
 *
 * <p>Recording runs in its own transaction ({@code REQUIRES_NEW}) for two reasons. A failed audit
 * write must never roll back the business action that succeeded — and, more importantly, an action
 * that fails and rolls back must still leave its attempt on the record. A trail that only shows
 * successes is the one an investigator cannot use.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /** @param diff "before → after", or null when the action changes no value */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActorPrincipal actor, String module, String action, String diff,
                       String targetType, String targetId) {
        try {
            auditLogRepository.save(AuditLogEntry.builder()
                    .actorId(actor == null ? null : actor.id())
                    .actorName(actor == null ? "system" : actor.displayName())
                    .module(module)
                    .action(action)
                    .diff(diff)
                    .targetType(targetType)
                    .targetId(targetId)
                    .build());
        } catch (Exception ex) {
            // Never let bookkeeping break the operation it was meant to describe.
            log.error("Could not write the audit entry [{} / {}]", module, action, ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(ActorPrincipal actor, String module, String action, String diff) {
        record(actor, module, action, diff, null, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> search(String actorName, String module, Instant from, Instant to) {
        return auditLogRepository.search(blankToNull(actorName), blankToNull(module), from, to);
    }

    /** Convenience for building the "before → after" string the admin UI displays verbatim. */
    public static String diff(Object before, Object after) {
        return before + " → " + after;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
