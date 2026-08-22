package com.kola.backend.credit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Chaque règle a un poids maximal. La somme de tous les poids = 100.
 * Pour chaque règle, le score partiel va de 0 à son poids max.
 *
 * REPONDÉRATION : l'ajout de LOAN_REPAYMENT_HISTORY (15 pts) a été financé par
 * DEPOSIT_REGULARITY (20 → 15) et TRANSACTION_VOLUME (15 → 10), pour garder la
 * somme à 100. Le modèle notait jusque-là l'activité du compte sans jamais
 * regarder si l'utilisateur avait remboursé ses prêts précédents : un
 * emprunteur en défaut pouvait conserver un score ELITE et son plafond de
 * 2 000 000 XOF. Les deux règles réduites sont celles qui mesuraient le plus
 * indirectement la solvabilité.
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
            15
    ),
    LOAN_REPAYMENT_HISTORY(
            "Historique de remboursement",
            "Les prêts déjà remboursés dans les délais sont le meilleur prédicteur du risque ; un défaut passé l'est tout autant.",
            15
    ),
    VAULT_DISCIPLINE(
            "Discipline d'épargne (coffres respectés)",
            "Maintenir un coffre jusqu'à l'échéance démontre la discipline financière.",
            15
    ),
    TRANSACTION_VOLUME(
            "Volume de transactions mensuel",
            "Un volume élevé reflète une activité économique réelle.",
            10
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
