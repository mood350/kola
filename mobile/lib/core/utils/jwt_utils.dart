import 'dart:convert';

/// Vérifie l'expiration d'un JWT en décodant localement son payload (claim `exp`),
/// sans appel réseau — il n'existe pas d'endpoint /api/auth/validate côté backend.
/// Retourne `true` si le token est absent, malformé, ou expiré (fail-safe : on
/// considère un token illisible comme expiré plutôt que de risquer un faux positif).
bool isJwtExpired(String token) {
  try {
    final parts = token.split('.');
    if (parts.length != 3) return true;

    final payload = _decodeBase64Url(parts[1]);
    final claims = jsonDecode(payload) as Map<String, dynamic>;
    final exp = claims['exp'];
    if (exp is! int) return true;

    final expiryDate = DateTime.fromMillisecondsSinceEpoch(
      exp * 1000,
      isUtc: true,
    );
    return DateTime.now().toUtc().isAfter(expiryDate);
  } catch (_) {
    return true;
  }
}

String _decodeBase64Url(String input) {
  var normalized = input.replaceAll('-', '+').replaceAll('_', '/');
  final padding = normalized.length % 4;
  if (padding > 0) {
    normalized += '=' * (4 - padding);
  }
  return utf8.decode(base64.decode(normalized));
}
