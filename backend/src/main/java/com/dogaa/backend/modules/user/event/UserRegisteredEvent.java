package com.dogaa.backend.modules.user.event;

import java.util.UUID;

/**
 * Raised once, when an account is created.
 *
 * <p>An event rather than a direct call: the wallet module provisions the current and savings
 * accounts in response, and auth has no business importing wallet to do it.
 */
public record UserRegisteredEvent(UUID userId) {
}
