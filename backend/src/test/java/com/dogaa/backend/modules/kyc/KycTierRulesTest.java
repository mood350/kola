package com.dogaa.backend.modules.kyc;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.kyc.service.KycTierRules;
import com.dogaa.backend.modules.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KycTierRulesTest {

    private static User user(boolean phoneVerified, boolean emailVerified) {
        User user = new User();
        user.setPhoneVerified(phoneVerified);
        user.setEmailVerified(emailVerified);
        return user;
    }

    @Test
    void aVerifiedPhoneAloneIsTier0() {
        assertThat(KycTierRules.resolve(user(true, false), Set.of())).isEqualTo(KycTier.TIER_0);
    }

    @Test
    void aVerifiedEmailReachesTier1() {
        assertThat(KycTierRules.resolve(user(true, true), Set.of())).isEqualTo(KycTier.TIER_1);
    }

    @Test
    void anApprovedIdentityDocumentReachesTier2() {
        assertThat(KycTierRules.resolve(user(true, true), Set.of(KycDocumentType.NATIONAL_ID)))
                .isEqualTo(KycTier.TIER_2);
    }

    @Test
    void aVoterCardCountsAsIdentityBecauseItIsOftenTheOnlyOneHeld() {
        assertThat(KycTierRules.resolve(user(true, true), Set.of(KycDocumentType.VOTER_CARD)))
                .isEqualTo(KycTier.TIER_2);
    }

    @Test
    void selfieAndProofOfAddressOnTopOfAnIdReachTier3() {
        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.PASSPORT,
                KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(user(true, true), approved)).isEqualTo(KycTier.TIER_3);
    }

    @Test
    void documentsDoNotSubstituteForTheEmailStep() {
        // The tiers are cumulative: an approved ID without a verified email stays at tier 0.
        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.NATIONAL_ID, KycDocumentType.SELFIE);
        assertThat(KycTierRules.resolve(user(true, false), approved)).isEqualTo(KycTier.TIER_0);
    }

    @Test
    void aSelfieWithoutAnIdentityDocumentDoesNotReachTier3() {
        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(user(true, true), approved)).isEqualTo(KycTier.TIER_1);
    }

    @Test
    void anUnverifiedPhoneCannotClimbAtAll() {
        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.PASSPORT,
                KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(user(false, true), approved)).isEqualTo(KycTier.TIER_0);
    }

    @Test
    void theRequirementsDescribeTheNextStepOnly() {
        assertThat(KycTierRules.requirementsForNextTier(user(true, false), Set.of()))
                .containsExactly("Verify your email address");
        assertThat(KycTierRules.requirementsForNextTier(user(true, true), Set.of()))
                .hasSize(1)
                .allSatisfy(requirement -> assertThat(requirement).contains("identity document"));
        assertThat(KycTierRules.requirementsForNextTier(user(true, true),
                Set.of(KycDocumentType.NATIONAL_ID, KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS)))
                .isEmpty();
    }
}
