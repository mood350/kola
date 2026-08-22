/// Fréquence d'un virement programmé (cf. ScheduledTransfer.Frequency backend).
enum ScheduleFrequency {
  monthly,
  weekly;

  static ScheduleFrequency fromBackend(String value) {
    switch (value) {
      case 'MONTHLY':
        return ScheduleFrequency.monthly;
      case 'WEEKLY':
        return ScheduleFrequency.weekly;
      default:
        return ScheduleFrequency.monthly;
    }
  }

  String toBackend() =>
      this == ScheduleFrequency.monthly ? 'MONTHLY' : 'WEEKLY';

  String get label =>
      this == ScheduleFrequency.monthly ? 'Mensuel' : 'Hebdomadaire';
}

/// Statut d'un virement programmé (cf. ScheduledTransfer.ScheduledStatus).
enum ScheduleStatus {
  active,
  paused,
  failedPermanently;

  static ScheduleStatus fromBackend(String value) {
    switch (value) {
      case 'ACTIVE':
        return ScheduleStatus.active;
      case 'PAUSED':
        return ScheduleStatus.paused;
      case 'FAILED_PERMANENTLY':
        return ScheduleStatus.failedPermanently;
      default:
        return ScheduleStatus.active;
    }
  }

  String get label {
    switch (this) {
      case ScheduleStatus.active:
        return 'Actif';
      case ScheduleStatus.paused:
        return 'En pause';
      case ScheduleStatus.failedPermanently:
        return 'En échec';
    }
  }
}

/// Modèle d'un virement programmé (cf. ScheduledTransferResponse backend).
class ScheduledTransfer {
  final int id;
  final ScheduleFrequency frequency;
  final int executionDay;
  final double amount;
  final String currency;
  final String? description;
  final ScheduleStatus status;
  final DateTime? lastExecutedAt;
  final DateTime? nextExecutionDate;
  final int? walletId;
  final int? targetVaultId;
  final String? targetVaultName;

  const ScheduledTransfer({
    required this.id,
    required this.frequency,
    required this.executionDay,
    required this.amount,
    required this.currency,
    this.description,
    required this.status,
    this.lastExecutedAt,
    this.nextExecutionDate,
    this.walletId,
    this.targetVaultId,
    this.targetVaultName,
  });

  /// Libellé lisible de la récurrence (ex : « le 15 de chaque mois »).
  String get scheduleLabel {
    if (frequency == ScheduleFrequency.monthly) {
      return 'Le $executionDay de chaque mois';
    }
    const jours = [
      'lundi',
      'mardi',
      'mercredi',
      'jeudi',
      'vendredi',
      'samedi',
      'dimanche',
    ];
    final index = (executionDay - 1).clamp(0, 6);
    return 'Chaque ${jours[index]}';
  }

  factory ScheduledTransfer.fromJson(Map<String, dynamic> json) {
    return ScheduledTransfer(
      id: json['id'] as int,
      frequency: ScheduleFrequency.fromBackend(
        json['frequency'] as String? ?? 'MONTHLY',
      ),
      executionDay: json['executionDay'] as int? ?? 1,
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
      currency: json['currency'] as String? ?? 'XOF',
      description: json['description'] as String?,
      status: ScheduleStatus.fromBackend(json['status'] as String? ?? 'ACTIVE'),
      lastExecutedAt: json['lastExecutedAt'] != null
          ? DateTime.tryParse(json['lastExecutedAt'] as String)
          : null,
      nextExecutionDate: json['nextExecutionDate'] != null
          ? DateTime.tryParse(json['nextExecutionDate'] as String)
          : null,
      walletId: json['walletId'] as int?,
      targetVaultId: json['targetVaultId'] as int?,
      targetVaultName: json['targetVaultName'] as String?,
    );
  }
}
