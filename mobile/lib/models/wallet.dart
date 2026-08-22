/// Modèle représentant un wallet Kola, tel que renvoyé par
/// GET /api/wallets et GET /api/wallets/{id} (cf. WalletResponse backend).
class Wallet {
  final int id;
  final String currency;
  final double balance;
  final double lockedBalance;
  final double availableBalance;
  final bool active;

  const Wallet({
    required this.id,
    required this.currency,
    required this.balance,
    required this.lockedBalance,
    required this.availableBalance,
    required this.active,
  });

  factory Wallet.fromJson(Map<String, dynamic> json) {
    return Wallet(
      id: json['id'] as int,
      currency: json['currency'] as String? ?? 'XOF',
      balance: (json['balance'] as num?)?.toDouble() ?? 0,
      lockedBalance: (json['lockedBalance'] as num?)?.toDouble() ?? 0,
      availableBalance: (json['availableBalance'] as num?)?.toDouble() ?? 0,
      active: json['active'] as bool? ?? true,
    );
  }
}
