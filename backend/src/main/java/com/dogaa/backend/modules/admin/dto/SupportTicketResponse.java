package com.dogaa.backend.modules.admin.dto;

/**
 * A ticket in the queue (BACKEND.md 13).
 *
 * @param ref    short reference, e.g. "#8821"
 * @param status {@code open}, {@code in_progress} or {@code resolved}
 */
public record SupportTicketResponse(String ref, String subject, String userName, String status) {
}
