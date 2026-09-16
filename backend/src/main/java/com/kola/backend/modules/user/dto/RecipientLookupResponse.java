package com.kola.backend.modules.user.dto;

import java.util.UUID;

/** Minimal identity returned before a P2P transfer confirmation. */
public record RecipientLookupResponse(UUID id, String displayName, String maskedPhone) {
}
