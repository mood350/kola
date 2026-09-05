package com.dogaa.backend.modules.assistant.dto;

import com.dogaa.backend.modules.assistant.entity.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, MessageRole role, String content, Instant createdAt) {
}
