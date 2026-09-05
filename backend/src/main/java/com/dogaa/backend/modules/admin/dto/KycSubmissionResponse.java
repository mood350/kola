package com.dogaa.backend.modules.admin.dto;

/**
 * One pending document in the review queue (BACKEND.md 6).
 *
 * @param id           the document id — what approve/reject and the file download are called with
 * @param fromTier     the tier the user holds today
 * @param toTier       the tier they would reach if this document is approved
 * @param documentType French label of what was sent, e.g. "Carte d'identité"
 * @param contentType  MIME type of the stored file, so the console knows whether it can show it
 *                     inline or must offer it as a download
 * @param fileName     the name the customer's file was uploaded under
 */
public record KycSubmissionResponse(String id,
                                    String name,
                                    String fromTier,
                                    String toTier,
                                    String receivedAt,
                                    String documentType,
                                    String contentType,
                                    String fileName) {
}
