package com.kola.backend.credit;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Ce qu'une personne peut réellement rembourser, d'après ses flux observés.
 *
 * ═══ POURQUOI CET OBJET N'EST PAS LE SCORE ═══
 *
 * Le score dit si l'on peut faire confiance. Celui-ci dit combien on peut
 * prêter sans mettre l'emprunteur en difficulté. Ce sont deux questions
 * distinctes, et les confondre produit exactement le défaut que ce module
 * corrige : deux personnes à 80 points recevaient le même plafond de palier,
 * qu'elles encaissent 20 000 ou 300 000 XOF par mois. La première se voyait
 * offrir un prêt qu'elle ne pouvait pas rembourser ; la seconde, un prêt
 * dérisoire au regard de son activité.
 *
 * Le score fixe la CONFIANCE (donc le taux, et un plafond absolu de sécurité).
 * Cet objet fixe le MONTANT.
 *
 * ═══ POURQUOI UN MONTANT PAR DURÉE ═══
 *
 * Le prêt Kola se rembourse en UNE FOIS à l'échéance : {@code montant × (1 +
 * taux × durée)}. Ce qui compte n'est donc pas une mensualité mais la capacité
 * à mettre de côté la somme due sur la période. Plus la durée est longue, plus
 * cette accumulation est possible — un montant unique n'aurait aucun sens. La
 * carte {@code maxAmountByDuration} donne le plafond pour chacune des durées
 * proposées, et l'interface lit celui de la durée choisie.
 *
 * @param monthlyInflow      entrées mensuelles moyennes observées
 * @param monthlyOutflow     sorties mensuelles moyennes observées
 * @param monthlyDisposable  ce qui reste réellement disponible chaque mois
 * @param activeMonths       mois, sur la période observée, ayant vu au moins une entrée
 * @param stabilityFactor    décote appliquée à des revenus irréguliers (0 à 1)
 * @param tierCeiling        plafond absolu du palier de score — jamais dépassé
 * @param graduationCeiling  plafond de progression : un premier prêt reste petit
 * @param maxAmountByDuration montant maximal pour chaque durée, en mois
 * @param limitingFactor     ce qui contraint réellement le montant, à afficher
 */
public record LoanCapacity(
        BigDecimal monthlyInflow,
        BigDecimal monthlyOutflow,
        BigDecimal monthlyDisposable,
        int activeMonths,
        BigDecimal stabilityFactor,
        BigDecimal tierCeiling,
        BigDecimal graduationCeiling,
        Map<Integer, BigDecimal> maxAmountByDuration,
        LimitingFactor limitingFactor
) {

    /**
     * Ce qui borne le montant.
     *
     * Affiché à l'emprunteur, parce qu'un plafond sans raison se vit comme un
     * refus arbitraire — alors que chacun de ces cas appelle une action
     * différente et lisible.
     */
    public enum LimitingFactor {
        /** Les flux observés : c'est la contrainte normale. */
        CASH_FLOW("Votre capacité de remboursement, calculée sur vos entrées et sorties récentes"),
        /** Le palier de score : la confiance, pas les moyens. */
        TIER("Le plafond de votre palier de score"),
        /** Premier prêt, ou progression : le montant grandit à chaque remboursement. */
        GRADUATION("Le plafond des premiers prêts — il augmente à chaque remboursement à l'heure"),
        /** Aucun flux exploitable : rien à prêter dessus. */
        NO_ACTIVITY("Activité insuffisante sur les 90 derniers jours pour évaluer une capacité");

        private final String label;

        LimitingFactor(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Montant maximal pour la durée demandée, ou zéro si la durée n'est pas proposée. */
    public BigDecimal maxAmountFor(int durationMonths) {
        return maxAmountByDuration.getOrDefault(durationMonths, BigDecimal.ZERO);
    }

    /** Vrai si aucun prêt n'est possible, quelle que soit la durée. */
    public boolean isEmpty() {
        return maxAmountByDuration.values().stream()
                .allMatch(amount -> amount.compareTo(BigDecimal.ZERO) <= 0);
    }
}
