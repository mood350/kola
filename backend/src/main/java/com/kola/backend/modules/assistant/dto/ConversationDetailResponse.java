package com.kola.backend.modules.assistant.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDetailResponse(UUID id,
                                         String title,
                                         Instant lastMessageAt,
                                         List<MessageResponse> messages) {
}
