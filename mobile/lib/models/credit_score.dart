/// Palier de crédit (cf. backend CreditTier).
enum CreditTier {
  ineligible,
  basic,
  standard,
  premium,
  elite;

  static CreditTier fromBackend(String value) {
    switch (value) {
      case 'INELIGIBLE':
        return CreditTier.ineligible;
      case 'BASIC':
        return CreditTier.basic;
      case 'STANDARD':
        return CreditTier.standard;
      case 'PREMIUM':
        return CreditTier.premium;
      case 'ELITE':
        return CreditTier.elite;
      default:
        return CreditTier.ineligible;
    }
  }

  String get label {
    switch (this) {
      case CreditTier.ineligible:
        return 'Non éligible';
      case CreditTier.basic:
        return 'Basique';
      case CreditTier.standard:
        return 'Standard';
      case CreditTier.premium:
        return 'Premium';
      case CreditTier.elite:
        return 'Élite';
    }
  }
}

/// Détail d'une règle de scoring (cf. ScoreBreakdown.RuleScore backend).
/// `label` et `explanation` sont déjà en français et prêts à l'affichage —
/// pas besoin de les redériver côté mobile depuis `rule`.
class RuleScore {
  final String rule; // ex: "ACCOUNT_SENIORITY", pour référence/debug uniquement
  final String label;
  final int points;
  final int maxPoints;
  final String explanation;

  const RuleScore({
    required this.rule,
    required this.label,
    required this.points,
    required this.maxPoints,
    required this.explanation,
  });

  factory RuleScore.fromJson(Map<String, dynamic> json) {
    return RuleScore(
      rule: json['rule'] as String? ?? '',
      label: json['label'] as String? ?? '',
      points: json['points'] as int? ?? 0,
      maxPoints: json['maxPoints'] as int? ?? 0,
      explanation: json['explanation'] as String? ?? '',
    );
  }
}

/// Score de crédit complet (cf. ScoreBreakdown backend).
/// Affichage uniquement : ne jamais recalculer ce score côté mobile,
/// le backend le fait déjà (CreditScoringService, mis en cache 30 jours).
class CreditScoreBreakdown {
  final int totalScore;
  final CreditTier tier;
  final double maxLoanAmount;
  final double monthlyRate;
  final DateTime? computedAt;
  final DateTime? expiresAt;
  final List<RuleScore> details;

  const CreditScoreBreakdown({
    required this.totalScore,
    required this.tier,
    required this.maxLoanAmount,
    required this.monthlyRate,
    this.computedAt,
    this.expiresAt,
    required this.details,
  });

  factory CreditScoreBreakdown.fromJson(Map<String, dynamic> json) {
    final rawDetails = json['details'] as List<dynamic>? ?? [];
    return CreditScoreBreakdown(
      totalScore: json['totalScore'] as int? ?? 0,
      tier: CreditTier.fromBackend(json['tier'] as String? ?? 'INELIGIBLE'),
      maxLoanAmount: (json['maxLoanAmount'] as num?)?.toDouble() ?? 0,
      monthlyRate: (json['monthlyRate'] as num?)?.toDouble() ?? 0,
      computedAt: json['computedAt'] != null
          ? DateTime.tryParse(json['computedAt'] as String)
          : null,
      expiresAt: json['expiresAt'] != null
          ? DateTime.tryParse(json['expiresAt'] as String)
          : null,
      details: rawDetails
          .map((e) => RuleScore.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
