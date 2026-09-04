package com.dogaa.backend.modules.audit.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One sensitive action, recorded for good.
 *
 * <p>Append-only by contract: nothing in the codebase updates or deletes a row here, and nothing
 * should. An audit trail that can be edited is not an audit trail. The regulator's question is
 * always the same — who changed what, when, and from what to what — so those four things are
 * columns rather than a free-text blob.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_created", columnList = "created_at"),
        @Index(name = "idx_audit_log_actor", columnList = "actor_id"),
        @Index(name = "idx_audit_log_module", columnList = "module")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntry extends BaseEntity {

    @Column(name = "actor_id")
    private UUID actorId;

    /** Denormalised on purpose: the trail must stay readable even if the account is later renamed. */
    @Column(name = "actor_name", nullable = false, length = 120)
    private String actorName;

    @Column(nullable = false, length = 40)
    private String module;

    @Column(nullable = false, length = 200)
    private String action;

    /** "before → after", pre-formatted by the caller. Null when the action changes no value. */
    @Column(length = 500)
    private String diff;

    /** What was acted upon, so an entry can be traced back to its subject. */
    @Column(name = "target_type", length = 60)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;
}
