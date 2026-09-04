package com.dogaa.backend.modules.kyc.service;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.user.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What each tier requires, in one place.
 *
 * <p>The tier is <em>derived</em> from verified facts, never assigned. An administrator approves a
 * document; they cannot hand someone a tier. That is the whole point of the rule set living here
 * rather than being spread over the endpoints that happen to promote a user.
 *
 * <p>Email plays no part in this ladder. Most users of a Mobile Money wallet in the UEMOA zone have
 * a phone and no mailbox, so gating a tier on an address would strand exactly the people the
 * product exists for. An address can still be added and verified from the profile, but it earns
 * nothing here.
 */
public final class KycTierRules {

    private KycTierRules() {
    }

    /**
     * @param approvedTypes document types already approved for the user
     * @return the highest tier the user actually qualifies for
     */
    public static KycTier resolve(User user, Set<KycDocumentType> approvedTypes) {
        if (!user.isPhoneVerified()) {
            // Cannot happen through the registration flow, which verifies by OTP first.
            return KycTier.TIER_0;
        }

        boolean hasIdentityDocument = approvedTypes.stream().anyMatch(KycDocumentType::isIdentityDocument);
        boolean fullyValidated = hasIdentityDocument
                && approvedTypes.contains(KycDocumentType.SELFIE)
                && approvedTypes.contains(KycDocumentType.PROOF_OF_ADDRESS);

        if (fullyValidated) {
            return KycTier.TIER_3;
        }
        if (hasIdentityDocument) {
            return KycTier.TIER_2;
        }
        if (isProfileComplete(user)) {
            return KycTier.TIER_1;
        }
        return KycTier.TIER_0;
    }

    /**
     * TIER_1 is the declarative step: the user states who they are and where they live, with nothing
     * to prove it yet. It raises the ceilings modestly, and it is what TIER_2 then verifies against
     * the identity document.
     */
    public static boolean isProfileComplete(User user) {
        return notBlank(user.getFirstName())
                && notBlank(user.getLastName())
                && user.getDateOfBirth() != null
                && notBlank(user.getAddress())
                && notBlank(user.getCity())
                && notBlank(user.getCountry());
    }

    /** Human-readable list of what still stands between the user and the next tier. */
    public static List<String> requirementsForNextTier(User user, Set<KycDocumentType> approvedTypes) {
        KycTier current = resolve(user, approvedTypes);
        return switch (current) {
            case TIER_0 -> List.of("Complete your profile: address, city and country");
            case TIER_1 -> List.of("Submit an identity document (national ID, passport, "
                    + "driving licence or voter card) and have it approved");
            case TIER_2 -> missingForFullValidation(approvedTypes);
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

    private static List<String> missingForFullValidation(Set<KycDocumentType> approvedTypes) {
        List<String> missing = new ArrayList<>(2);
        if (!approvedTypes.contains(KycDocumentType.SELFIE)) {
            missing.add("Submit a selfie");
        }
        if (!approvedTypes.contains(KycDocumentType.PROOF_OF_ADDRESS)) {
            missing.add("Submit a proof of address");
        }
        return List.copyOf(missing);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
