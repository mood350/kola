/// Capacité d'emprunt (cf. LoanCapacityResponse backend).
///
/// ═══ À NE PAS CONFONDRE AVEC `CreditScoreBreakdown.maxLoanAmount` ═══
///
/// Le score dit la SOLVABILITÉ : il fixe le taux et le plafond du palier. Cette
/// classe dit le MONTANT, calculé sur les flux réels des 90 derniers jours.
/// Deux personnes au même score obtiennent des valeurs différentes ici si leurs
/// entrées et sorties diffèrent.
///
/// L'écran de demande doit borner le montant sur [maxAmountFor], jamais sur le
/// plafond du palier : ce dernier est presque toujours hors d'atteinte, et le
/// backend refuserait la demande.
class LoanCapacity {
  final double monthlyInflow;
  final double monthlyOutflow;
  final double monthlyDisposable;

  /// Mois, sur les 3 observés, ayant vu au moins une entrée.
  final int activeMonths;

  /// Décote appliquée à des revenus irréguliers (0 à 1).
  final double stabilityFactor;

  final double tierCeiling;
  final double graduationCeiling;

  /// Montant maximal par durée. Clés JSON : les mois sous forme de chaînes.
  final Map<int, double> maxAmountByDuration;

  /// Ce qui borne réellement le montant : CASH_FLOW, TIER, GRADUATION, NO_ACTIVITY.
  final String limitingFactor;
  final String limitingFactorLabel;

  /// Au-delà, la demande passe en examen manuel : aucun versement immédiat.
  final double manualReviewThreshold;

  const LoanCapacity({
    required this.monthlyInflow,
    required this.monthlyOutflow,
    required this.monthlyDisposable,
    required this.activeMonths,
    required this.stabilityFactor,
    required this.tierCeiling,
    required this.graduationCeiling,
    required this.maxAmountByDuration,
    required this.limitingFactor,
    required this.limitingFactorLabel,
    required this.manualReviewThreshold,
  });

  /// Montant maximal pour la durée demandée, ou 0 si elle n'est pas proposée.
  double maxAmountFor(int durationMonths) =>
      maxAmountByDuration[durationMonths] ?? 0;

  /// Vrai si aucun prêt n'est possible, quelle que soit la durée.
  bool get isEmpty => maxAmountByDuration.values.every((amount) => amount <= 0);

  factory LoanCapacity.fromJson(Map<String, dynamic> json) {
    final rawAmounts =
        json['maxAmountByDuration'] as Map<String, dynamic>? ?? {};

    return LoanCapacity(
      monthlyInflow: (json['monthlyInflow'] as num?)?.toDouble() ?? 0,
      monthlyOutflow: (json['monthlyOutflow'] as num?)?.toDouble() ?? 0,
      monthlyDisposable: (json['monthlyDisposable'] as num?)?.toDouble() ?? 0,
      activeMonths: json['activeMonths'] as int? ?? 0,
      stabilityFactor: (json['stabilityFactor'] as num?)?.toDouble() ?? 0,
      tierCeiling: (json['tierCeiling'] as num?)?.toDouble() ?? 0,
      graduationCeiling: (json['graduationCeiling'] as num?)?.toDouble() ?? 0,
      // Les clés arrivent en chaînes (JSON n'a pas de clé numérique) : on les
      // reconvertit une fois ici plutôt qu'à chaque lecture dans les écrans.
      maxAmountByDuration: rawAmounts.map(
        (months, amount) =>
            MapEntry(int.parse(months), (amount as num).toDouble()),
      ),
      limitingFactor: json['limitingFactor'] as String? ?? '',
      limitingFactorLabel: json['limitingFactorLabel'] as String? ?? '',
      manualReviewThreshold:
          (json['manualReviewThreshold'] as num?)?.toDouble() ?? 0,
    );
  }
}
