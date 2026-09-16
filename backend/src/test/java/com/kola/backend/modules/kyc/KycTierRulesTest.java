package com.kola.backend.modules.kyc;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.modules.kyc.entity.KycDocumentType;
import com.kola.backend.modules.kyc.service.KycTierRules;
import com.kola.backend.modules.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KycTierRulesTest {

    /** A registered user: identity declared at sign-up, nothing else filled in yet. */
    private static User registered() {
        User user = new User();
        user.setPhoneVerified(true);
        user.setFirstName("Kossi");
        user.setLastName("Adjo");
        user.setDateOfBirth(LocalDate.of(1995, 4, 12));
        return user;
    }

    private static User withCompleteProfile() {
        User user = registered();
        user.setAddress("Rue de la Paix");
        user.setCity("Lome");
        user.setCountry("TG");
        return user;
    }

    @Test
    void aVerifiedPhoneAloneIsTier0() {
        assertThat(KycTierRules.resolve(registered(), Set.of())).isEqualTo(KycTier.TIER_0);
    }

    @Test
    void aCompleteProfileReachesTier1() {
        assertThat(KycTierRules.resolve(withCompleteProfile(), Set.of())).isEqualTo(KycTier.TIER_1);
    }

    @Test
    void anApprovedIdentityDocumentReachesTier2() {
        assertThat(KycTierRules.resolve(withCompleteProfile(), Set.of(KycDocumentType.NATIONAL_ID)))
                .isEqualTo(KycTier.TIER_2);
    }

    @Test
    void aVoterCardCountsAsIdentityBecauseItIsOftenTheOnlyOneHeld() {
        assertThat(KycTierRules.resolve(withCompleteProfile(), Set.of(KycDocumentType.VOTER_CARD)))
                .isEqualTo(KycTier.TIER_2);
    }

    @Test
    void selfieAndProofOfAddressOnTopOfAnIdReachTier3() {
        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.PASSPORT,
                KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(withCompleteProfile(), approved)).isEqualTo(KycTier.TIER_3);
    }

    @Test
    void anApprovedDocumentCarriesAnIncompleteProfilePastTier1() {
        // A verified document is stronger evidence than a declared address, so it does not wait
        // for the declarative step to be filled in.
        assertThat(KycTierRules.resolve(registered(), Set.of(KycDocumentType.NATIONAL_ID)))
                .isEqualTo(KycTier.TIER_2);
    }

    @Test
    void anUnverifiedEmailIsNoObstacleAtAnyLevel() {
        User user = withCompleteProfile();
        user.setEmail("kossi@example.com");
        user.setEmailVerified(false);

        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.PASSPORT,
                KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(user, approved)).isEqualTo(KycTier.TIER_3);
    }

    @Test
    void aSelfieWithoutAnIdentityDocumentDoesNotReachTier3() {
        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(withCompleteProfile(), approved)).isEqualTo(KycTier.TIER_1);
    }

    @Test
    void anUnverifiedPhoneCannotClimbAtAll() {
        User user = withCompleteProfile();
        user.setPhoneVerified(false);

        Set<KycDocumentType> approved = EnumSet.of(KycDocumentType.PASSPORT,
                KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS);
        assertThat(KycTierRules.resolve(user, approved)).isEqualTo(KycTier.TIER_0);
    }

    @Test
    void aProfileMissingOneFieldIsNotComplete() {
        User user = withCompleteProfile();
        user.setCity("   ");

        assertThat(KycTierRules.isProfileComplete(user)).isFalse();
        assertThat(KycTierRules.resolve(user, Set.of())).isEqualTo(KycTier.TIER_0);
    }

    @Test
    void theRequirementsDescribeTheNextStepOnly() {
        assertThat(KycTierRules.requirementsForNextTier(registered(), Set.of()))
                .containsExactly("Complete your profile: address, city and country");

        assertThat(KycTierRules.requirementsForNextTier(withCompleteProfile(), Set.of()))
                .hasSize(1)
                .allSatisfy(requirement -> assertThat(requirement).contains("identity document"));

        assertThat(KycTierRules.requirementsForNextTier(withCompleteProfile(),
                Set.of(KycDocumentType.NATIONAL_ID, KycDocumentType.SELFIE)))
                .containsExactly("Submit a proof of address");

        assertThat(KycTierRules.requirementsForNextTier(withCompleteProfile(),
                Set.of(KycDocumentType.NATIONAL_ID, KycDocumentType.SELFIE, KycDocumentType.PROOF_OF_ADDRESS)))
                .isEmpty();
    }
}
