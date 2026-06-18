package com.kola.backend.transaction;

import com.kola.backend.user.KycLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║              TransactionPolicy.java                         ║
 * ║   Limites KYC + calcul des frais, centralisés ici           ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Toutes les limites sont exprimées en XOF (devise pivot de référence).
 * Dans une implémentation complète, ces valeurs viendraient d'une table
 * de configuration en BDD (modifiable sans redéploiement) ; elles sont
 * codées en dur ici pour rester simple et explicite dans le cadre du
 * mémoire.
 *
 * Limites journalières d'envoi (TRANSFER_OUT + WITHDRAWAL cumulés) :
 *  - TIER_0 (téléphone seulement)     : 50 000 XOF / jour
 *  - TIER_1 (email vérifié)           : 200 000 XOF / jour
 *  - TIER_2 (pièce d'identité)        : 1 000 000 XOF / jour
 *  - TIER_3 (identité validée)        : pas de limite (Long.MAX_VALUE)
 */
public final class TransactionPolicy {

    private TransactionPolicy() {
    }

    private static final Map<KycLevel, BigDecimal> DAILY_LIMITS_XOF = Map.of(
            KycLevel.TIER_0, new BigDecimal("50000"),
            KycLevel.TIER_1, new BigDecimal("200000"),
            KycLevel.TIER_2, new BigDecimal("1000000"),
            KycLevel.TIER_3, new BigDecimal("999999999")
    );

    // Taux de frais appliqué sur les transferts (1.5%) et retraits (1%).
    // Les dépôts sont gratuits (incitation à recharger le wallet).
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.015");
    private static final BigDecimal WITHDRAWAL_FEE_RATE = new BigDecimal("0.01");

    public static BigDecimal getDailyLimit(KycLevel level) {
        return DAILY_LIMITS_XOF.getOrDefault(level, BigDecimal.ZERO);
    }

    public static BigDecimal computeTransferFee(BigDecimal amount) {
        return amount.multiply(TRANSFER_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal computeWithdrawalFee(BigDecimal amount) {
        return amount.multiply(WITHDRAWAL_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
    }
}
