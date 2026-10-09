package com.kola.backend.modules.assistant.dto;

import com.kola.backend.modules.assistant.entity.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, MessageRole role, String content, Instant createdAt) {
}
