package com.dogaa.backend.modules.assistant.mapper;

import com.dogaa.backend.modules.assistant.dto.ConversationDetailResponse;
import com.dogaa.backend.modules.assistant.dto.ConversationResponse;
import com.dogaa.backend.modules.assistant.dto.MessageResponse;
import com.dogaa.backend.modules.assistant.entity.AssistantConversation;
import com.dogaa.backend.modules.assistant.entity.AssistantMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AssistantMapper {

    public MessageResponse toResponse(AssistantMessage message) {
        return new MessageResponse(message.getId(), message.getRole(), message.getContent(),
                message.getCreatedAt());
    }

    public ConversationResponse toResponse(AssistantConversation conversation) {
        return new ConversationResponse(conversation.getId(), conversation.getTitle(),
                conversation.getLastMessageAt());
    }

    public ConversationDetailResponse toDetail(AssistantConversation conversation,
                                               List<AssistantMessage> messages) {
        return new ConversationDetailResponse(conversation.getId(), conversation.getTitle(),
                conversation.getLastMessageAt(), messages.stream().map(this::toResponse).toList());
    }
}
