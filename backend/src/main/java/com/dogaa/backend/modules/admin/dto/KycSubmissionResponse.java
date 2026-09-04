package com.dogaa.backend.modules.admin.dto;

/**
 * One pending document in the review queue (BACKEND.md 6).
 *
 * @param id       the document id — what approve/reject is called with
 * @param fromTier the tier the user holds today
 * @param toTier   the tier they would reach if this document is approved
 */
public record KycSubmissionResponse(String id,
                                    String name,
                                    String fromTier,
                                    String toTier,
                                    String receivedAt) {
}
