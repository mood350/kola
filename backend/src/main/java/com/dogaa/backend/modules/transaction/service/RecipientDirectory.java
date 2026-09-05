package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.util.PhoneNumbers;
import com.dogaa.backend.config.AuthProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.TooManyRequestsException;
import com.dogaa.backend.modules.transaction.dto.RecipientLookupResponse;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Answers "whose number is this?" so a sender can confirm before the money moves.
 *
 * <p>A P2P transfer cannot be undone without opening a dispute, so this screen is the last chance
 * to catch a mistyped digit. That makes the lookup worth having — and worth restraining.
 *
 * <p><b>It is an enumeration surface, and it is treated as one.</b> A phone-to-name endpoint walked
 * in a loop is a way to harvest the names behind a numbering plan. Three things bound it: the
 * caller must be signed in, each caller gets a limited number of lookups per hour, and the response
 * carries the name and nothing else — no id, no tier, no balance, no account age. Anyone patient
 * enough to spend a real account's hourly budget learns names they could already have learned by
 * attempting transfers, which is the floor this feature cannot go below while remaining useful.
 *
 * <p>The counter is in memory, so it is per instance: two servers give a caller two budgets. That
 * is honest for the single-node deployment this runs on today; move it to a shared store before
 * scaling out, or the limit quietly multiplies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipientDirectory {

    /** Enough for a person choosing a recipient, far too few to walk a numbering plan. */
    private static final int MAX_LOOKUPS_PER_WINDOW = 60;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<UUID, Deque<Instant>> lookups = new ConcurrentHashMap<>();

    private final AuthProperties authProperties;
    private final UserService userService;

    public RecipientLookupResponse lookup(UUID callerId, String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BadRequestException("Numéro de téléphone manquant");
        }

        // Normalised with the same default calling code the transfer path uses, so the
        // number confirmed here and the number paid are the same account.
        String phone = PhoneNumbers.normalize(rawPhone, authProperties.getDefaultCallingCode());
        recordLookup(callerId);

        Optional<User> recipient = userService.findByPhone(phone);
        if (recipient.isEmpty()) {
            // Not an error: an unknown number is payable through Mobile Money. The client needs to
            // know there is no name to confirm, which is different from the lookup having failed.
            return new RecipientLookupResponse(phone, PhoneNumbers.mask(phone), false, null, false);
        }

        User user = recipient.get();
        return new RecipientLookupResponse(phone, PhoneNumbers.mask(phone), true,
                displayName(user), user.getId().equals(callerId));
    }

    /**
     * The name as it should appear on a confirmation screen, or null when the account has none.
     *
     * <p>Null rather than a placeholder: the screen must be able to say "ce compte n'affiche pas de
     * nom" instead of showing a reassuring stand-in for a check that did not actually happen.
     */
    public String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().strip();
        String last = user.getLastName() == null ? "" : user.getLastName().strip();
        String full = (first + " " + last).strip();
        return full.isEmpty() ? null : full;
    }

    /** The name behind a number, for stamping onto a transaction. Absent when unknown. */
    public Optional<String> nameFor(String phone) {
        return userService.findByPhone(phone).map(this::displayName);
    }

    private void recordLookup(UUID callerId) {
        Instant now = Instant.now();
        Deque<Instant> recent = lookups.computeIfAbsent(callerId, id -> new ArrayDeque<>());

        synchronized (recent) {
            Instant cutoff = now.minus(WINDOW);
            while (!recent.isEmpty() && recent.peekFirst().isBefore(cutoff)) {
                recent.pollFirst();
            }
            if (recent.size() >= MAX_LOOKUPS_PER_WINDOW) {
                log.warn("Recipient lookup rate limit hit by user {}", callerId);
                throw new TooManyRequestsException(
                        "Trop de vérifications de numéro. Réessayez dans quelques minutes.");
            }
            recent.addLast(now);
        }
    }
}
