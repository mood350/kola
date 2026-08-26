package com.kola.backend.credit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le barème doit totaliser exactement 100 points.
 *
 * POURQUOI CE TEST EXISTE : les plafonds déclarés totalisaient 105, et le code
 * pouvait en produire 115, écrêtés à 100. Ces 15 points de mou permettaient
 * d'atteindre le haut du barème en échouant complètement à une règle entière —
 * un critère qu'on peut ignorer ne sélectionne plus rien.
 *
 * Ce test est la sentinelle de cette propriété : quiconque ajoute une règle ou
 * modifie un poids doit rééquilibrer l'ensemble, et le saura immédiatement.
 */
class ScoringRuleWeightsTest {

    @Test
    void lesPlafondsTotalisentExactementCent() {
        assertThat(ScoringRule.totalMaxPoints())
                .as("un barème sur 100 dont les règles totalisent autre chose n'a plus de sens")
                .isEqualTo(100);
    }

    @Test
    void lHistoriqueDeRemboursementEstLeCritereLePlusLourd() {
        int repayment = ScoringRule.LOAN_REPAYMENT_HISTORY.getMaxPoints();

        for (ScoringRule rule : ScoringRule.values()) {
            assertThat(repayment)
                    .as("le seul signal directement observé de ce qu'on cherche à prédire "
                            + "doit peser au moins autant que ses indices indirects (%s)", rule.name())
                    .isGreaterThanOrEqualTo(rule.getMaxPoints());
        }
    }

    @Test
    void aucuneRegleNePeseAssezPourImposerUnPalier() {
        // ELITE commence à 90 : aucune règle isolée ne doit pouvoir en fournir
        // une part telle que le reste devienne accessoire.
        for (ScoringRule rule : ScoringRule.values()) {
            assertThat(rule.getMaxPoints())
                    .as("règle %s trop lourde : le score deviendrait mono-critère", rule.name())
                    .isLessThanOrEqualTo(25);
        }
    }
}
