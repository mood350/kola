package com.kola.backend.modules.assistant.repository;

import com.kola.backend.modules.assistant.entity.AssistantMessage;
import com.kola.backend.modules.assistant.entity.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, UUID> {

    List<AssistantMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /** What the daily quota counts: the user's own questions, across every thread. */
    long countByUserIdAndRoleAndCreatedAtAfter(UUID userId, MessageRole role, Instant since);

    void deleteByConversationId(UUID conversationId);
}
