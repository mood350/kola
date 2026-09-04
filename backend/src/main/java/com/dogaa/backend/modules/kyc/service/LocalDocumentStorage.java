package com.dogaa.backend.modules.kyc.service;

import com.dogaa.backend.config.KycProperties;
import com.dogaa.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Development storage: files land in a local directory.
 *
 * <p>Not fit for production — no encryption at rest, no access audit, and the files sit next to the
 * application. Provide another {@link DocumentStorage} bean before going live.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnMissingBean(ignored = LocalDocumentStorage.class, value = DocumentStorage.class)
public class LocalDocumentStorage implements DocumentStorage {

    private final KycProperties kycProperties;

    @Override
    public String store(MultipartFile file, String ownerReference) {
        String key = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store the KYC document", ex);
        }
        log.info("Stored KYC document {} for {}", key, ownerReference);
        return key;
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read the KYC document " + storageKey, ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ex) {
            log.warn("Could not delete the KYC document {}", storageKey, ex);
        }
    }

    /**
     * Resolves inside the configured directory and refuses to leave it. Storage keys are generated
     * here, but this method also serves keys read back from the database, so it must not trust them.
     */
    private Path resolve(String storageKey) {
        Path root = Paths.get(kycProperties.getUpload().getStorageDirectory()).toAbsolutePath().normalize();
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid document reference");
        }
        return target;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String extension = filename.substring(dot + 1).toLowerCase();
        return extension.matches("[a-z0-9]{1,8}") ? "." + extension : "";
    }
}
