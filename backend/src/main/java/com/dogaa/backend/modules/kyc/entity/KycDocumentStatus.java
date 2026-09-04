package com.dogaa.backend.modules.kyc.entity;

public enum KycDocumentStatus {
    /** Uploaded, waiting for a human review in the back-office. */
    PENDING,
    APPROVED,
    REJECTED
}
