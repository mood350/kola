package com.dogaa.backend.modules.assistant.controller;

import com.dogaa.backend.common.dto.ApiResponse;
import com.dogaa.backend.modules.assistant.dto.AskRequest;
import com.dogaa.backend.modules.assistant.dto.AssistantReplyResponse;
import com.dogaa.backend.modules.assistant.dto.ConversationDetailResponse;
import com.dogaa.backend.modules.assistant.dto.ConversationResponse;
import com.dogaa.backend.modules.assistant.service.AssistantService;
import com.dogaa.backend.modules.auth.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The in-app assistant.
 *
 * <p>Every route works on the caller alone: the user id comes from the token and appears in no
 * request body or path. There is deliberately no route to ask about another account, and no
 * administrator view — an assistant that could read any customer's balances on request would be a
 * far better tool for a compromised admin session than for a support agent.
 */
@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant", description = "Assistant conversationnel de l'application")
@SecurityRequirement(name = "bearerAuth")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/messages")
    @Operation(summary = "Poser une question à l'assistant")
    public ResponseEntity<ApiResponse<AssistantReplyResponse>> ask(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AskRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assistantService.ask(currentUser.id(), request)));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Ses propres conversations, la plus récente en premier")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> conversations(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                assistantService.listConversations(currentUser.id())));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Les messages d'une conversation")
    public ResponseEntity<ApiResponse<ConversationDetailResponse>> conversation(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(ApiResponse.ok(
                assistantService.getConversation(currentUser.id(), conversationId)));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Supprimer une conversation")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID conversationId) {
        assistantService.deleteConversation(currentUser.id(), conversationId);
        return ResponseEntity.ok(ApiResponse.ok("Conversation supprimée"));
    }
}
