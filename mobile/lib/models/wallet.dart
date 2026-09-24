import 'json.dart';

/// Portefeuille : compte courant (`CURRENT`) ou épargne Bankivi (`SAVINGS`).
class Wallet {
  const Wallet({
    required this.id,
    required this.currency,
    required this.type,
    required this.availableBalance,
    required this.lockedBalance,
    required this.totalBalance,
    required this.status,
  });

  final String id;
  final String currency;
  final String type;
  final double availableBalance;
  final double lockedBalance;
  final double totalBalance;
  final String status;

  bool get isSavings => type == 'SAVINGS';

  factory Wallet.fromJson(Map<String, dynamic> json) {
    return Wallet(
      id: asString(json['id']),
      currency: asString(json['currency'], 'XOF'),
      type: asString(json['type'], 'CURRENT'),
      availableBalance: asDouble(json['availableBalance']),
      lockedBalance: asDouble(json['lockedBalance']),
      totalBalance: asDouble(json['totalBalance']),
      status: asString(json['status']),
    );
  }
}
