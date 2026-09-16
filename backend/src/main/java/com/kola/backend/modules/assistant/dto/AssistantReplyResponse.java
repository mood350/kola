package com.kola.backend.modules.assistant.dto;

import java.util.UUID;

/**
 * The answer to one question.
 *
 * @param remainingToday what is left of the daily quota, so the app can warn before the wall
 */
public record AssistantReplyResponse(UUID conversationId,
                                     String title,
                                     MessageResponse answer,
                                     int remainingToday) {
}
