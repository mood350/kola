/// Lecteurs tolérants pour le JSON du backend.
///
/// Les montants voyagent en `BigDecimal` : Jackson les sérialise tantôt en
/// nombre, tantôt en chaîne selon le champ. Les parser au même endroit évite
/// que chaque modèle réinvente sa propre conversion — et qu'un solde s'affiche
/// à zéro parce qu'il est arrivé entre guillemets.
double asDouble(dynamic value, [double fallback = 0]) {
  if (value == null) return fallback;
  if (value is num) return value.toDouble();
  return double.tryParse(value.toString()) ?? fallback;
}

int asInt(dynamic value, [int fallback = 0]) {
  if (value == null) return fallback;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString()) ?? fallback;
}

String asString(dynamic value, [String fallback = '']) {
  return value?.toString() ?? fallback;
}

String? asStringOrNull(dynamic value) {
  final text = value?.toString();
  return (text == null || text.isEmpty) ? null : text;
}

bool asBool(dynamic value, [bool fallback = false]) {
  if (value == null) return fallback;
  if (value is bool) return value;
  return value.toString().toLowerCase() == 'true';
}

DateTime? asDate(dynamic value) {
  final text = asStringOrNull(value);
  if (text == null) return null;
  return DateTime.tryParse(text)?.toLocal();
}

List<String> asStringList(dynamic value) {
  if (value is! List) return const [];
  return value.map((item) => item.toString()).toList();
}

List<T> asList<T>(dynamic value, T Function(Map<String, dynamic>) parse) {
  if (value is! List) return const [];
  return value
      .whereType<Map>()
      .map((item) => parse(item.cast<String, dynamic>()))
      .toList();
}
