package com.kola.backend.credit;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Capacité d'emprunt, telle qu'exposée au client.
 *
 * ═══ POURQUOI ON RENVOIE LE DÉTAIL ET PAS SEULEMENT UN MONTANT ═══
 *
 * Un plafond sans explication se vit comme un refus arbitraire. En montrant les
 * entrées, les sorties et le disponible qui ont servi au calcul, on rend le
 * montant vérifiable par l'emprunteur — il reconnaît ses propres chiffres — et
 * on lui indique le levier : augmenter ses entrées, réduire ses sorties, ou
 * rembourser un premier prêt pour lever le plafond de progression.
 *
 * C'est le même parti pris que le détail du score, qui explique chaque règle
 * plutôt que d'asséner une note.
 */
public record LoanCapacityResponse(
        BigDecimal monthlyInflow,
        BigDecimal monthlyOutflow,
        BigDecimal monthlyDisposable,
        int activeMonths,
        BigDecimal stabilityFactor,
        BigDecimal tierCeiling,
        BigDecimal graduationCeiling,
        /** Montant maximal par durée, en mois : {1: …, 2: …, … 12: …}. */
        Map<Integer, BigDecimal> maxAmountByDuration,
        String limitingFactor,
        String limitingFactorLabel,
        /** Rappel du palier : il fixe le taux et le plafond absolu, jamais le montant. */
        CreditTier tier,
        BigDecimal monthlyRate,
        /**
         * Montant au-delà duquel un humain tranche.
         *
         * Exposé parce que l'interface doit dire la vérité AVANT l'envoi : une
         * demande au-dessus de ce seuil n'est pas versée immédiatement, et
         * promettre le contraire ferait attendre un virement qui ne vient pas.
         */
        BigDecimal manualReviewThreshold
) {

    public static LoanCapacityResponse from(LoanCapacity capacity, CreditTier tier) {
        return new LoanCapacityResponse(
                capacity.monthlyInflow(),
                capacity.monthlyOutflow(),
                capacity.monthlyDisposable(),
                capacity.activeMonths(),
                capacity.stabilityFactor(),
                capacity.tierCeiling(),
                capacity.graduationCeiling(),
                capacity.maxAmountByDuration(),
                capacity.limitingFactor().name(),
                capacity.limitingFactor().getLabel(),
                tier,
                tier.getMonthlyRate(),
                LoanService.manualReviewThreshold()
        );
    }
}
