package com.kola.backend.modules.assistant.entity;

import com.kola.backend.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One chat thread between a customer and the assistant.
 *
 * <p>Holds a plain {@code userId} rather than a relation to {@code User}: the assistant module does
 * not own customer data, it only reads it through the owning services.
 */
@Entity
@Table(name = "assistant_conversations",
        indexes = @Index(name = "idx_assistant_conversation_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
public class AssistantConversation extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Taken from the opening question, so the history list is readable without opening a thread. */
    @Column(nullable = false, length = 80)
    private String title;

    /** Bumped on every turn: the thread list is sorted on this, not on {@code updatedAt}. */
    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    public AssistantConversation(UUID userId, String title) {
        this.userId = userId;
        this.title = title;
        this.lastMessageAt = Instant.now();
    }
}
