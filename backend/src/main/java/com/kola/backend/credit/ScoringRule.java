package com.kola.backend.credit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Les huit règles qui composent le score de solvabilité.
 *
 * ═══ CE QUE CE SCORE MESURE, ET CE QU'IL NE MESURE PAS ═══
 *
 * Il mesure la PROBABILITÉ QU'UNE PERSONNE REMBOURSE. Rien d'autre. Il ne dit
 * pas combien elle peut emprunter — c'est le travail de
 * {@link RepaymentCapacityService}, qui lit ses flux réels. Deux personnes à 80
 * ont la même fiabilité présumée et méritent le même taux ; celle qui encaisse
 * 300 000 XOF par mois n'a pas pour autant la même capacité de remboursement
 * que celle qui en encaisse 20 000.
 *
 * ═══ LA SOMME DES PLAFONDS FAIT EXACTEMENT 100 ═══
 *
 * Et c'est une contrainte, pas une coïncidence : elle est vérifiée par un test.
 * Elle valait 105 auparavant, et le code pouvait produire jusqu'à 115 points
 * écrêtés à 100 — deux règles dépassaient leur propre plafond déclaré
 * (régularité des dépôts en donnait 20 pour 15 annoncés, volume 15 pour 10).
 * Ces 15 points de mou permettaient d'atteindre le sommet du barème en
 * échouant complètement à une règle entière, ce qui vide le score de son sens :
 * un barème dont on peut ignorer un critère ne sélectionne plus sur ce critère.
 *
 * ═══ POURQUOI CES POIDS ═══
 *
 * L'historique de remboursement pèse le plus (20) : c'est le seul signal
 * directement observé de ce que l'on cherche à prédire, tout le reste n'en est
 * qu'un indice. En contrepartie, quelqu'un qui n'a jamais emprunté reçoit une
 * note neutre (8/20) et plafonne mécaniquement à 88 — soit PREMIUM, jamais
 * ÉLITE. Le palier le plus haut se mérite en remboursant, il ne s'obtient pas
 * en ouvrant un compte bien tenu. C'est le prêt progressif, inscrit dans le
 * barème lui-même.
 *
 * La diversité du réseau de bénéficiaires tombe à 5 : c'est le signal le plus
 * facile à fabriquer (il suffit d'enregistrer cinq numéros) et le plus faible
 * en prédiction. Un critère qu'on peut simuler ne doit pas peser lourd.
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
            20
    ),
    VAULT_DISCIPLINE(
            "Discipline d'épargne (coffres respectés)",
            "Maintenir un coffre jusqu'à l'échéance démontre la discipline financière.",
            10
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
            5
    );

    private final String label;
    private final String description;
    private final int maxPoints;

    /** Total des plafonds. Doit valoir 100 — un test le garantit. */
    public static int totalMaxPoints() {
        int total = 0;
        for (ScoringRule rule : values()) {
            total += rule.getMaxPoints();
        }
        return total;
    }
}
