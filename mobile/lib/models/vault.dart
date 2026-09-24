import 'json.dart';

/// Coffre-fort d'épargne fléchée vers un objectif.
class Vault {
  const Vault({
    required this.id,
    required this.name,
    required this.currency,
    required this.balance,
    required this.targetAmount,
    required this.progressPercent,
    required this.goalReached,
    required this.status,
    this.targetDate,
    this.description,
    this.createdAt,
  });

  final String id;
  final String name;
  final String currency;
  final double balance;
  final double targetAmount;
  final double progressPercent;
  final bool goalReached;
  final String status;
  final DateTime? targetDate;
  final String? description;
  final DateTime? createdAt;

  factory Vault.fromJson(Map<String, dynamic> json) {
    return Vault(
      id: asString(json['id']),
      name: asString(json['name']),
      currency: asString(json['currency'], 'XOF'),
      balance: asDouble(json['balance']),
      targetAmount: asDouble(json['targetAmount']),
      progressPercent: asDouble(json['progressPercent']),
      goalReached: asBool(json['goalReached']),
      status: asString(json['status']),
      targetDate: asDate(json['targetDate']),
      description: asStringOrNull(json['description']),
      createdAt: asDate(json['createdAt']),
    );
  }
}
