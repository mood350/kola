package com.dogaa.backend.modules.assistant.entity;

/**
 * Who wrote a stored message.
 *
 * <p>There is deliberately no {@code SYSTEM} value: the briefing is rebuilt from live data on every
 * call and never persisted. Storing it would freeze a stale balance into the conversation and
 * replay it forever.
 */
public enum MessageRole {
    USER,
    ASSISTANT
}
