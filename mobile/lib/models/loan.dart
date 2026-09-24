import 'json.dart';

/// Micro-prêt adossé à l'épargne, tel que renvoyé par `/api/v1/credit/loans`.
class Loan {
  const Loan({
    required this.id,
    required this.currency,
    required this.principal,
    required this.collateralAmount,
    required this.leverageRatio,
    required this.monthlyRatePercent,
    required this.interestAmount,
    required this.penaltyAmount,
    required this.totalDue,
    required this.amountRepaid,
    required this.outstanding,
    required this.shortfallAmount,
    required this.status,
    required this.scoreAtGrant,
    this.disbursedAt,
    this.dueAt,
    this.settledAt,
  });

  final String id;
  final String currency;
  final double principal;
  final double collateralAmount;
  final double leverageRatio;
  final double monthlyRatePercent;
  final double interestAmount;
  final double penaltyAmount;
  final double totalDue;
  final double amountRepaid;
  final double outstanding;
  final double shortfallAmount;
  final String status;
  final int scoreAtGrant;
  final DateTime? disbursedAt;
  final DateTime? dueAt;
  final DateTime? settledAt;

  bool get isActive => status == 'ACTIVE' || status == 'OVERDUE';

  /// Part déjà remboursée, en pourcentage.
  double get repaidPercent =>
      totalDue <= 0 ? 0 : (amountRepaid / totalDue * 100).clamp(0, 100);

  /// Jours restants avant échéance ; négatif si le prêt est en retard.
  int? get daysLeft {
    final due = dueAt;
    if (due == null) return null;
    return due.difference(DateTime.now()).inDays;
  }

  factory Loan.fromJson(Map<String, dynamic> json) {
    return Loan(
      id: asString(json['id']),
      currency: asString(json['currency'], 'XOF'),
      principal: asDouble(json['principal']),
      collateralAmount: asDouble(json['collateralAmount']),
      leverageRatio: asDouble(json['leverageRatio']),
      monthlyRatePercent: asDouble(json['monthlyRatePercent']),
      interestAmount: asDouble(json['interestAmount']),
      penaltyAmount: asDouble(json['penaltyAmount']),
      totalDue: asDouble(json['totalDue']),
      amountRepaid: asDouble(json['amountRepaid']),
      outstanding: asDouble(json['outstanding']),
      shortfallAmount: asDouble(json['shortfallAmount']),
      status: asString(json['status']),
      scoreAtGrant: asInt(json['scoreAtGrant']),
      disbursedAt: asDate(json['disbursedAt']),
      dueAt: asDate(json['dueAt']),
      settledAt: asDate(json['settledAt']),
    );
  }
}

/// Capacité d'emprunt du client : score, plafond, et ce qui bloque encore.
class CreditEligibility {
  const CreditEligibility({
    required this.eligible,
    required this.score,
    required this.minimumScore,
    required this.kycTier,
    required this.currency,
    required this.savingsBalance,
    required this.leverageRatio,
    required this.maxLoanAmount,
    required this.monthlyRatePercent,
    required this.totalRepayable,
    required this.termDays,
    required this.loansRepaid,
    required this.blockers,
  });

  final bool eligible;
  final int score;
  final int minimumScore;
  final String kycTier;
  final String currency;
  final double savingsBalance;
  final double leverageRatio;
  final double maxLoanAmount;
  final double monthlyRatePercent;
  final double totalRepayable;
  final int termDays;
  final int loansRepaid;
  final List<String> blockers;

  /// Palier de lecture du score, tel qu'affiché sur l'écran Crédit.
  String get scoreLabel {
    if (score >= 80) return 'Excellent';
    if (score >= 65) return 'Très bon';
    if (score >= 50) return 'Bon';
    if (score >= 35) return 'Moyen';
    return 'À construire';
  }

  factory CreditEligibility.fromJson(Map<String, dynamic> json) {
    return CreditEligibility(
      eligible: asBool(json['eligible']),
      score: asInt(json['score']),
      minimumScore: asInt(json['minimumScore']),
      kycTier: asString(json['kycTier'], 'TIER_0'),
      currency: asString(json['currency'], 'XOF'),
      savingsBalance: asDouble(json['savingsBalance']),
      leverageRatio: asDouble(json['leverageRatio']),
      maxLoanAmount: asDouble(json['maxLoanAmount']),
      monthlyRatePercent: asDouble(json['monthlyRatePercent']),
      totalRepayable: asDouble(json['totalRepayable']),
      termDays: asInt(json['termDays']),
      loansRepaid: asInt(json['loansRepaid']),
      blockers: asStringList(json['blockers']),
    );
  }
}
