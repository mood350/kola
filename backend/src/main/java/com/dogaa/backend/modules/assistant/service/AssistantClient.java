package com.dogaa.backend.modules.assistant.service;

import java.util.List;

/**
 * The language-model seam, mirroring {@code OtpSender} in the notification module: the service
 * above it knows nothing about which provider answers, and swapping provider touches one class.
 */
public interface AssistantClient {

    /** True when the client is configured well enough to be called at all. */
    boolean isAvailable();

    /**
     * @param systemPrompt the grounding data — product rules and the caller's own situation
     * @param conversation the turns so far, oldest first, ending with the new question
     * @return the assistant's answer
     */
    String complete(String systemPrompt, List<Turn> conversation);

    /** One turn. {@code role} is the provider-neutral "user" or "assistant". */
    record Turn(String role, String content) {

        public static Turn user(String content) {
            return new Turn("user", content);
        }

        public static Turn assistant(String content) {
            return new Turn("assistant", content);
        }
    }
}
