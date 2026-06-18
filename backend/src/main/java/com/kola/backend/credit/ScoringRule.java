package com.kola.backend.credit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Chaque règle a un poids maximal. La somme de tous les poids = 100.
 * Pour chaque règle, le score partiel va de 0 à son poids max.
 */
@Getter
@RequiredArgsConstructor
public enum ScoringRule {

    ACCOUNT_SENIORITY(
            "Ancienneté du compte",
            "Un compte utilisé depuis plus de 3 mois montre un engagement durable.",
            10
    ),
    KYC_LEVEL(
            "Niveau de vérification KYC",
            "Un niveau KYC élevé réduit le risque d'identité.",
            15
    ),
    DEPOSIT_REGULARITY(
            "Régularité des dépôts (30 derniers jours)",
            "Des dépôts réguliers indiquent des revenus stables.",
            20
    ),
    VAULT_DISCIPLINE(
            "Discipline d'épargne (coffres respectés)",
            "Maintenir un coffre jusqu'à l'échéance démontre la discipline financière.",
            15
    ),
    TRANSACTION_VOLUME(
            "Volume de transactions mensuel",
            "Un volume élevé reflète une activité économique réelle.",
            15
    ),
    EXPENSE_INCOME_RATIO(
            "Ratio dépenses/revenus",
            "Un ratio sain montre une gestion prudente du budget.",
            15
    ),
    BENEFICIARY_DIVERSITY(
            "Diversité du réseau de bénéficiaires",
            "Un réseau actif et varié signale une activité commerciale.",
            10
    );

    private final String label;
    private final String description;
    private final int maxPoints;
}
