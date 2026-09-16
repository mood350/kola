package com.kola.backend.modules.admin.entity;

import com.kola.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A customer support request (BACKEND.md 13).
 *
 * <p>Addressed by its short reference ("#8821") rather than its id: that is the number the agent
 * reads back to the customer on the phone, so it is what every endpoint takes.
 */
@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_reference", columnList = "reference", unique = true),
        @Index(name = "idx_ticket_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket extends BaseEntity {

    @Column(nullable = false, unique = true, length = 16)
    private String reference;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    /** The agent who took it, once someone has. */
    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;
}
