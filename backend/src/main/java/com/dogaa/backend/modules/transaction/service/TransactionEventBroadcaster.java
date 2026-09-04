package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.modules.transaction.dto.TransactionResponse;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.mapper.TransactionMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Live feed of transaction events (DOGAA.md 4.5: "en temps reel"), one Server-Sent-Events
 * stream per connected user — plain HTTP, no extra dependency, no message broker.
 *
 * <p>In-memory only: emitters live on this application instance. Fine for a single node
 * (every other "dev sender" in this project — OTP, email, external payouts — makes the
 * same simplification); a multi-instance deployment would need a shared channel (e.g.
 * Redis pub/sub) fanning events out to every instance's local emitters.
 */
@Component
public class TransactionEventBroadcaster {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes per connection

    private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final TransactionMapper transactionMapper;

    public TransactionEventBroadcaster(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    /** Registers a new live subscriber; the caller (the controller) returns this straight to Spring MVC. */
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> emitters = emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    /** Pushes the transaction to every live subscriber on either side of it. */
    public void publish(Transaction tx) {
        TransactionResponse payload = transactionMapper.toResponse(tx);
        send(tx.getSenderId(), payload);
        if (tx.getRecipientId() != null && !tx.getRecipientId().equals(tx.getSenderId())) {
            send(tx.getRecipientId(), payload);
        }
    }

    private void send(UUID userId, TransactionResponse payload) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("transaction").data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
    }
}
