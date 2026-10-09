import 'json.dart';

/// Écriture au registre : transfert, recharge, paiement, mouvement de coffre.
class KolaTransaction {
  const KolaTransaction({
    required this.id,
    required this.reference,
    required this.type,
    required this.status,
    required this.currency,
    required this.amount,
    required this.fee,
    required this.totalDebited,
    required this.createdAt,
    this.counterparty,
    this.description,
    this.failureReason,
    this.completedAt,
  });

  final String id;
  final String reference;
  final String type;
  final String status;
  final String currency;
  final double amount;
  final double fee;
  final double totalDebited;
  final DateTime createdAt;
  final String? counterparty;
  final String? description;
  final String? failureReason;
  final DateTime? completedAt;

  bool get isCompleted => status == 'COMPLETED';
  bool get isFailed => status == 'FAILED';
  bool get isPending => status == 'PENDING';

  /// Horodatage à afficher : la date d'exécution si elle existe, sinon celle
  /// de création (une opération en attente n'a pas encore abouti).
  DateTime get displayedAt => completedAt ?? createdAt;

  factory KolaTransaction.fromJson(Map<String, dynamic> json) {
    return KolaTransaction(
      id: asString(json['id']),
      reference: asString(json['reference']),
      type: asString(json['type']),
      status: asString(json['status']),
      currency: asString(json['currency'], 'XOF'),
      amount: asDouble(json['amount']),
      fee: asDouble(json['fee']),
      totalDebited: asDouble(json['totalDebited']),
      createdAt: asDate(json['createdAt']) ?? DateTime.now(),
      counterparty: asStringOrNull(json['counterparty']),
      description: asStringOrNull(json['description']),
      failureReason: asStringOrNull(json['failureReason']),
      completedAt: asDate(json['completedAt']),
    );
  }
}

/// Devis de frais renvoyé avant confirmation d'une opération.
class FeeQuote {
  const FeeQuote({
    required this.currency,
    required this.amount,
    required this.fee,
    required this.total,
  });

  final String currency;
  final double amount;
  final double fee;
  final double total;

  factory FeeQuote.fromJson(Map<String, dynamic> json) {
    return FeeQuote(
      currency: asString(json['currency'], 'XOF'),
      amount: asDouble(json['amount']),
      fee: asDouble(json['fee']),
      total: asDouble(json['total']),
    );
  }
}
