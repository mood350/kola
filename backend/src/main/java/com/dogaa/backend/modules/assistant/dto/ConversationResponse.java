package com.dogaa.backend.modules.assistant.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(UUID id, String title, Instant lastMessageAt) {
}
