import 'dart:convert';
import 'package:http/http.dart' as http;
import '../core/constants/app_constants.dart';
import '../models/user.dart';

/// Résultat générique pour les appels d'authentification.
class AuthResult {
  final bool success;
  final String? token;
  final String? refreshToken;
  final User? user;
  final String? errorMessage;

  AuthResult.ok({this.token, this.refreshToken, this.user})
      : success = true,
        errorMessage = null;

  AuthResult.fail(this.errorMessage)
      : success = false,
        token = null,
        refreshToken = null,
        user = null;
}

/// Service gérant les appels API liés à l'authentification.
class AuthService {
  Future<AuthResult> login({
    required String phoneNumber,
    required String password,
  }) async {
    try {
      final response = await http
          .post(
        Uri.parse('${AppConstants.baseUrl}/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'phoneNumber': phoneNumber,
          'password': password,
        }),
      )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return AuthResult.ok(
          token: data['accessToken'] as String?,
          refreshToken: data['refreshToken'] as String?,
          user: User.fromJson(data['user'] as Map<String, dynamic>),
        );
      } else {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return AuthResult.fail(data['message'] as String? ?? 'Échec de connexion');
      }
    } catch (e) {
      return AuthResult.fail('Erreur réseau : impossible de contacter le serveur');
    }
  }

  Future<AuthResult> register({
    required String fullName,
    required String phoneNumber,
    required String password,
  }) async {
    try {
      final response = await http
          .post(
        Uri.parse('${AppConstants.baseUrl}/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'fullName': fullName,
          'phoneNumber': phoneNumber,
          'password': password,
        }),
      )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return AuthResult.ok(
          token: data['accessToken'] as String?,
          refreshToken: data['refreshToken'] as String?,
          user: User.fromJson(data['user'] as Map<String, dynamic>),
        );
      } else {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return AuthResult.fail(data['message'] as String? ?? 'Échec de l\'inscription');
      }
    } catch (e) {
      return AuthResult.fail('Erreur réseau : impossible de contacter le serveur');
    }
  }
}