package com.kola.backend.modules.kyc.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Where identity documents are kept. The seam exists so production can move them to object
 * storage with encryption at rest and short-lived signed URLs, without touching the KYC logic.
 */
public interface DocumentStorage {

    /** @return an opaque key that {@link #load} can resolve later */
    String store(MultipartFile file, String ownerReference);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
