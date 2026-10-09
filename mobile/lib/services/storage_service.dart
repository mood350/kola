import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../core/constants/app_constants.dart';
import '../models/user.dart';

/// Service centralisant l'accès au stockage sécurisé (tokens, données sensibles).
class StorageService {
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  Future<void> saveToken(String token) async {
    await _storage.write(key: AppConstants.tokenKey, value: token);
  }

  Future<String?> getToken() async {
    return _storage.read(key: AppConstants.tokenKey);
  }

  Future<void> saveRefreshToken(String token) async {
    await _storage.write(key: AppConstants.refreshTokenKey, value: token);
  }

  Future<String?> getRefreshToken() async {
    return _storage.read(key: AppConstants.refreshTokenKey);
  }

  /// Profil mis en cache : il permet d'afficher l'écran de déverrouillage
  /// (prénom, numéro) avant le moindre appel réseau.
  Future<void> saveUser(KolaUser user) async {
    await _storage.write(
      key: AppConstants.userKey,
      value: jsonEncode(user.toJson()),
    );
  }

  Future<KolaUser?> readUser() async {
    final raw = await _storage.read(key: AppConstants.userKey);
    if (raw == null || raw.isEmpty) return null;
    try {
      return KolaUser.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      // Format d'une version antérieure : on repart d'une session propre.
      return null;
    }
  }

  Future<void> clearAll() async {
    await _storage.deleteAll();
  }
}
