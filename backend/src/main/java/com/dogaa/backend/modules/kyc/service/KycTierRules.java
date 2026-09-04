package com.dogaa.backend.modules.kyc.service;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.user.entity.User;

import java.util.List;
import java.util.Set;

/**
 * What each tier requires, in one place.
 *
 * <p>The tier is <em>derived</em> from verified facts, never assigned. An administrator approves a
 * document; they cannot hand someone a tier. That is the whole point of the rule set living here
 * rather than being spread over the endpoints that happen to promote a user.
 */
public final class KycTierRules {

    private KycTierRules() {
    }

    /**
     * @param approvedTypes document types already approved for the user
     * @return the highest tier the user actually qualifies for
     */
    public static KycTier resolve(User user, Set<KycDocumentType> approvedTypes) {
        boolean hasIdentityDocument = approvedTypes.stream().anyMatch(KycDocumentType::isIdentityDocument);
        boolean fullyValidated = hasIdentityDocument
                && approvedTypes.contains(KycDocumentType.SELFIE)
                && approvedTypes.contains(KycDocumentType.PROOF_OF_ADDRESS);

        if (!user.isPhoneVerified()) {
            // Cannot happen through the registration flow, which verifies by OTP first.
            return KycTier.TIER_0;
        }
        if (fullyValidated && user.isEmailVerified()) {
            return KycTier.TIER_3;
        }
        if (hasIdentityDocument && user.isEmailVerified()) {
            return KycTier.TIER_2;
        }
        if (user.isEmailVerified()) {
            return KycTier.TIER_1;
        }
        return KycTier.TIER_0;
    }

    /** Human-readable list of what still stands between the user and the next tier. */
    public static List<String> requirementsForNextTier(User user, Set<KycDocumentType> approvedTypes) {
        KycTier current = resolve(user, approvedTypes);
        return switch (current) {
            case TIER_0 -> List.of("Verify your email address");
            case TIER_1 -> List.of("Submit an identity document (national ID, passport, "
                    + "driving licence or voter card) and have it approved");
            case TIER_2 -> List.of("Submit a selfie", "Submit a proof of address");
            case TIER_3 -> List.of();
        };
    }

    public static KycTier nextTier(KycTier current) {
        return switch (current) {
            case TIER_0 -> KycTier.TIER_1;
            case TIER_1 -> KycTier.TIER_2;
            case TIER_2, TIER_3 -> KycTier.TIER_3;
        };
    }
}
