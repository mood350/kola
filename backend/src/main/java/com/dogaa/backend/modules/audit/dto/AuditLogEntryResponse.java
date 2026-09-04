package com.dogaa.backend.modules.audit.dto;

/**
 * Shape consumed as-is by the admin back-office (BACKEND.md 11): no envelope, no nesting.
 *
 * @param admin the actor's display name
 * @param diff  "before → after", already formatted server-side
 * @param time  ISO-8601 instant
 */
public record AuditLogEntryResponse(String id,
                                    String admin,
                                    String action,
                                    String diff,
                                    String time) {
}
