/// Modèle représentant un marchand (cf. MerchantResponse backend).
/// Jamais de solde exposé — juste de quoi confirmer au payeur qui il paie.
class Merchant {
  final int id;
  final String name;
  final String? category;
  final String merchantCode;

  const Merchant({
    required this.id,
    required this.name,
    this.category,
    required this.merchantCode,
  });

  factory Merchant.fromJson(Map<String, dynamic> json) {
    return Merchant(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      category: json['category'] as String?,
      merchantCode: json['merchantCode'] as String? ?? '',
    );
  }
}
