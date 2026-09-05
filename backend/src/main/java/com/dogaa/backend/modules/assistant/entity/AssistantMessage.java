package com.dogaa.backend.modules.assistant.entity;

import com.dogaa.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One turn of a conversation.
 *
 * <p>{@code userId} is duplicated here rather than reached through the conversation: the daily
 * quota counts a user's messages across every thread, and that count must not need a join.
 */
@Entity
@Table(name = "assistant_messages", indexes = {
        @Index(name = "idx_assistant_message_conversation", columnList = "conversation_id"),
        @Index(name = "idx_assistant_message_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AssistantMessage extends BaseEntity {

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageRole role;

    @Column(nullable = false, length = 8000)
    private String content;

    public AssistantMessage(UUID conversationId, UUID userId, MessageRole role, String content) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.content = content;
    }
}
