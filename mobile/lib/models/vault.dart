/// Statut d'un coffre-fort (cf. backend VaultStatus).
/// ACTIVE → fonds bloqués ; UNLOCKED → disponible ; CLOSED → fermé avant échéance.
enum VaultStatus {
  active,
  unlocked,
  closed;

  static VaultStatus fromBackend(String value) {
    switch (value) {
      case 'ACTIVE':
        return VaultStatus.active;
      case 'UNLOCKED':
        return VaultStatus.unlocked;
      case 'CLOSED':
        return VaultStatus.closed;
      default:
        return VaultStatus.active;
    }
  }
}

/// Modèle représentant un coffre-fort d'épargne (cf. VaultResponse backend).
/// Un coffre n'est jamais supprimé, seulement transitionné vers un autre statut.
class Vault {
  final int id;
  final String name;
  final String? purpose;
  final double? targetAmount;
  final double currentAmount;
  final String currency;
  final DateTime? unlockDate;
  final VaultStatus status;
  final int walletId;

  const Vault({
    required this.id,
    required this.name,
    this.purpose,
    this.targetAmount,
    required this.currentAmount,
    required this.currency,
    this.unlockDate,
    required this.status,
    required this.walletId,
  });

  factory Vault.fromJson(Map<String, dynamic> json) {
    return Vault(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      purpose: json['purpose'] as String?,
      targetAmount: (json['targetAmount'] as num?)?.toDouble(),
      currentAmount: (json['currentAmount'] as num?)?.toDouble() ?? 0,
      currency: json['currency'] as String? ?? 'XOF',
      unlockDate: json['unlockDate'] != null
          ? DateTime.tryParse(json['unlockDate'] as String)
          : null,
      status: VaultStatus.fromBackend(json['status'] as String? ?? 'ACTIVE'),
      walletId: json['walletId'] as int,
    );
  }
}
