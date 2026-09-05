package com.dogaa.backend.modules.assistant;

import com.dogaa.backend.config.AuthProperties;
import com.dogaa.backend.config.CreditProperties;
import com.dogaa.backend.config.DisputeProperties;
import com.dogaa.backend.config.FeeProperties;
import com.dogaa.backend.config.KycProperties;
import com.dogaa.backend.config.OtpProperties;
import com.dogaa.backend.config.ScoringProperties;
import com.dogaa.backend.modules.assistant.service.ProductKnowledge;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The briefing's one job is to stay true. These tests are about drift, not prose: they check that
 * the figures come from the configuration beans rather than from a paragraph someone typed once.
 */
class ProductKnowledgeTest {

    private final FeeProperties fees = new FeeProperties();
    private final KycProperties kyc = new KycProperties();
    private final CreditProperties credit = new CreditProperties();
    private final ScoringProperties scoring = new ScoringProperties();
    private final OtpProperties otp = new OtpProperties();
    private final AuthProperties auth = new AuthProperties();
    private final DisputeProperties disputes = new DisputeProperties();

    private final ProductKnowledge knowledge =
            new ProductKnowledge(fees, kyc, credit, scoring, otp, auth, disputes);

    @Test
    void theBriefingQuotesTheConfiguredFeesAndNotAHardCodedRate() {
        assertThat(knowledge.briefing()).contains("1.5 %");

        fees.setP2pPercent(new BigDecimal("2.25"));

        assertThat(knowledge.briefing())
                .contains("2.25 %")
                .doesNotContain("1.5 %");
    }

    /**
     * The reason the briefing is rebuilt on every call: an administrator editing the lending ladder
     * through the back-office must change what the assistant tells the next customer.
     */
    @Test
    void editingTheLendingLadderChangesWhatTheAssistantWillSay() {
        assertThat(knowledge.briefing()).contains("1.6");

        credit.setLadder(List.of(new CreditProperties.Rung(
                0, 50, new BigDecimal("1.1"), new BigDecimal("9.0"), new BigDecimal("25000"))));

        String briefing = knowledge.briefing();
        assertThat(briefing).contains("1.1").contains("9 %");
        assertThat(briefing).doesNotContain("1.6");
    }

    /** A null ceiling means unlimited. Printing it as "0 XOF" would tell a TIER_3 user the opposite. */
    @Test
    void anAbsentCeilingReadsAsUnlimitedNotAsZero() {
        String briefing = knowledge.briefing();

        assertThat(briefing).contains("illimité");
        assertThat(briefing).doesNotContain("plafonné à 0 XOF");
    }

    @Test
    void everyKycTierIsDescribedWithItsOwnLimits() {
        String briefing = knowledge.briefing();

        assertThat(briefing).contains("TIER_0", "TIER_1", "TIER_2", "TIER_3");
        assertThat(briefing).contains("par opération", "par jour", "par mois");
    }

    /**
     * The three anti-gaming mechanisms are the answer to "comment je fais monter mon score" — the
     * single most likely question the assistant will get, and the one where a wrong answer teaches
     * a user to game the model.
     */
    @Test
    void theBriefingExplainsWhyTheScoreCannotBeStaged() {
        String briefing = knowledge.briefing();

        assertThat(briefing)
                .contains("proportion")
                .contains("lissé")
                .contains(scoring.getMaterialityThreshold().toPlainString().split("\\.")[0]);
        assertThat(briefing).contains("Il n'y a pas de raccourci");
    }

    @Test
    void theBriefingSaysTheOtpIsMandatoryAndThatEmailIsNot() {
        String briefing = knowledge.briefing();

        assertThat(briefing).contains("le code OTP est obligatoire");
        assertThat(briefing).contains("facultatif");
    }
}
