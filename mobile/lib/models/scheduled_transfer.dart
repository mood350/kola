import 'json.dart';

/// Opération programmée : virement, versement au coffre, paiement de facture
/// ou abonnement récurrent.
class ScheduledTask {
  const ScheduledTask({
    required this.id,
    required this.type,
    required this.frequency,
    required this.amount,
    required this.currency,
    required this.beneficiaryReference,
    required this.status,
    required this.occurrencesCompleted,
    required this.retryCount,
    this.description,
    this.nextRunAt,
    this.endDate,
    this.maxOccurrences,
    this.lastFailureReason,
    this.fundingVaultId,
    this.biller,
    this.dayOfMonth,
  });

  final String id;
  final String type;
  final String frequency;
  final double amount;
  final String currency;
  final String beneficiaryReference;
  final String status;
  final int occurrencesCompleted;
  final int retryCount;
  final String? description;
  final DateTime? nextRunAt;
  final DateTime? endDate;
  final int? maxOccurrences;
  final String? lastFailureReason;
  final String? fundingVaultId;
  final String? biller;
  final int? dayOfMonth;

  bool get isActive => status == 'ACTIVE';
  bool get isPaused => status == 'PAUSED';

  static const Map<String, String> _frequencyLabels = {
    'DAILY': 'Quotidien',
    'WEEKLY': 'Hebdomadaire',
    'BIWEEKLY': 'Toutes les 2 semaines',
    'MONTHLY': 'Mensuel',
    'QUARTERLY': 'Trimestriel',
    'YEARLY': 'Annuel',
  };

  static const Map<String, String> _typeLabels = {
    'VAULT_DEPOSIT': 'Versement au coffre',
    'P2P_TRANSFER': 'Virement P2P',
    'MERCHANT_PAYMENT': 'Paiement marchand',
    'BILL_PAYMENT': 'Paiement de facture',
  };

  String get frequencyLabel => _frequencyLabels[frequency] ?? frequency;
  String get typeLabel => _typeLabels[type] ?? type;

  factory ScheduledTask.fromJson(Map<String, dynamic> json) {
    return ScheduledTask(
      id: asString(json['id']),
      type: asString(json['type']),
      frequency: asString(json['frequency']),
      amount: asDouble(json['amount']),
      currency: asString(json['currency'], 'XOF'),
      beneficiaryReference: asString(json['beneficiaryReference']),
      status: asString(json['status']),
      occurrencesCompleted: asInt(json['occurrencesCompleted']),
      retryCount: asInt(json['retryCount']),
      description: asStringOrNull(json['description']),
      nextRunAt: asDate(json['nextRunAt']),
      endDate: asDate(json['endDate']),
      maxOccurrences: json['maxOccurrences'] == null
          ? null
          : asInt(json['maxOccurrences']),
      lastFailureReason: asStringOrNull(json['lastFailureReason']),
      fundingVaultId: asStringOrNull(json['fundingVaultId']),
      biller: asStringOrNull(json['biller']),
      dayOfMonth: json['dayOfMonth'] == null ? null : asInt(json['dayOfMonth']),
    );
  }
}

/// Facturier disponible (CEET, TdE, CANAL+...), avec la forme attendue de la
/// référence client.
class Biller {
  const Biller({
    required this.code,
    required this.displayName,
    required this.identifierLabel,
    required this.identifierKind,
    required this.minLength,
    required this.maxLength,
    required this.fixedAmount,
  });

  final String code;
  final String displayName;
  final String identifierLabel;
  final String identifierKind;
  final int minLength;
  final int maxLength;
  final bool fixedAmount;

  bool get digitsOnly => identifierKind == 'DIGITS';

  factory Biller.fromJson(Map<String, dynamic> json) {
    return Biller(
      code: asString(json['code']),
      displayName: asString(json['displayName']),
      identifierLabel: asString(json['identifierLabel'], 'Référence'),
      identifierKind: asString(json['identifierKind'], 'ALPHANUMERIC'),
      minLength: asInt(json['minLength'], 1),
      maxLength: asInt(json['maxLength'], 32),
      fixedAmount: asBool(json['fixedAmount']),
    );
  }
}
