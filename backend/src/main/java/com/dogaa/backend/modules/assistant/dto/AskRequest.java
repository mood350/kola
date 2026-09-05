package com.dogaa.backend.modules.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * A question.
 *
 * <p>There is no user id: the caller is read from the token. Nothing in this body decides whose
 * money the assistant is allowed to describe.
 *
 * @param conversationId an existing thread to continue, or null to open a new one
 */
public record AskRequest(
        UUID conversationId,

        @NotBlank(message = "La question ne peut pas être vide")
        @Size(max = 1000, message = "La question est trop longue (1000 caractères maximum)")
        String message) {
}
