package com.dogaa.backend.modules.user.event;

import java.util.UUID;

/**
 * Raised when profile fields change.
 *
 * <p>An event rather than a direct call: the KYC tier depends on profile completeness, but the user
 * module must not import the KYC module, which already imports it. The listener lives in
 * {@code KycService}.
 */
public record UserProfileUpdatedEvent(UUID userId) {
}
