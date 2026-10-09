package com.kola.backend.modules.assistant.repository;

import com.kola.backend.modules.assistant.entity.AssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, UUID> {

    List<AssistantConversation> findByUserIdOrderByLastMessageAtDesc(UUID userId);

    /**
     * Loading by id <em>and</em> owner in one query is what keeps a thread private: there is no
     * moment where the service holds someone else's conversation and has to remember to check.
     */
    Optional<AssistantConversation> findByIdAndUserId(UUID id, UUID userId);
}
