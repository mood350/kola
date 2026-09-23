/// Opérateur Mobile Money encaissable ou payable
/// (cf. PaymentMethodController.PaymentMethod backend).
///
/// `code` est le nom de l'enum Java (`MOOV_TOGO`), jamais le code FedaPay
/// (`moov_tg`) : c'est ce que les endpoints attendent, et le code du
/// prestataire ne sort pas du backend.
class PaymentMethod {
  final String code;
  final String label;
  final String countryCode;

  const PaymentMethod({
    required this.code,
    required this.label,
    required this.countryCode,
  });

  factory PaymentMethod.fromJson(Map<String, dynamic> json) {
    return PaymentMethod(
      code: json['code'] as String? ?? '',
      label: json['label'] as String? ?? '',
      countryCode: json['countryCode'] as String? ?? '',
    );
  }
}

/// Ce que le backend propose comme moyens de paiement.
///
/// La liste n'est PAS écrite en dur côté mobile : les neuf opérateurs et leurs
/// codes vivent dans une seule énumération Java. Les recopier ici créerait une
/// seconde source de vérité, et un dépôt qui échoue sur le seul mobile le jour
/// où FedaPay renomme un canal.
class PaymentMethods {
  /// Opérateurs du pays retenu — ce que l'écran propose par défaut.
  final List<PaymentMethod> available;

  /// Tous les opérateurs, pour qui retire dans un autre pays.
  final List<PaymentMethod> all;

  final String countryCode;

  /// Faux si le prestataire n'est pas configuré : seul le mode test est alors
  /// possible, et l'écran doit le dire avant de proposer un opérateur.
  final bool providerEnabled;

  const PaymentMethods({
    required this.available,
    required this.all,
    required this.countryCode,
    required this.providerEnabled,
  });

  factory PaymentMethods.fromJson(Map<String, dynamic> json) {
    List<PaymentMethod> parse(String key) => (json[key] as List<dynamic>? ?? [])
        .map((e) => PaymentMethod.fromJson(e as Map<String, dynamic>))
        .toList();

    return PaymentMethods(
      available: parse('available'),
      all: parse('all'),
      countryCode: json['countryCode'] as String? ?? '',
      providerEnabled: json['providerEnabled'] as bool? ?? false,
    );
  }
}
