import 'dart:convert';
import 'package:http/http.dart' as http;
import '../core/constants/app_constants.dart';

/// Résultat générique pour les appels d'authentification qui renvoient des tokens.
class AuthTokenResult {
  final bool success;
  final String? accessToken;
  final String? refreshToken;
  final String? errorMessage;

  /// Vrai quand la requête n'a jamais atteint le serveur (timeout, avion,
  /// tunnel). À distinguer d'un refus du backend : sur un renouvellement de
  /// token, SessionManager doit purger la session dans le second cas et
  /// surtout pas dans le premier — sinon perdre le réseau trois secondes
  /// déconnecte un utilisateur dont les tokens sont parfaitement valides.
  final bool isNetworkFailure;

  AuthTokenResult.ok({this.accessToken, this.refreshToken})
    : success = true,
      errorMessage = null,
      isNetworkFailure = false;

  AuthTokenResult.fail(this.errorMessage)
    : success = false,
      accessToken = null,
      refreshToken = null,
      isNetworkFailure = false;

  AuthTokenResult.networkFailure()
    : success = false,
      accessToken = null,
      refreshToken = null,
      isNetworkFailure = true,
      errorMessage = 'Erreur réseau : impossible de contacter le serveur';
}

/// Résultat générique pour les appels qui ne renvoient pas de body (register, etc.)
class AuthActionResult {
  final bool success;
  final String? errorMessage;

  AuthActionResult.ok() : success = true, errorMessage = null;
  AuthActionResult.fail(this.errorMessage) : success = false;
}

/// Service gérant les appels API liés à l'authentification.
/// Les noms de champs et la forme des requêtes/réponses correspondent
/// EXACTEMENT aux DTOs Spring Boot : AuthenticationRequest, RegistrationRequest,
/// AuthenticationResponse (cf. com.kola.backend.auth.*).
///
/// TODO(dette-technique) : migrer vers ApiClient (lib/services/api_client.dart)
/// pour parser l'ErrorResponse backend de façon uniforme (code/details), comme
/// le font déjà les services Vault/Credit/User/Transaction/Beneficiary. Non fait
/// dans cette passe pour ne pas risquer de régresser le seul flux (login/lockout/
/// register-202/refresh) qui fonctionne déjà de bout en bout.
class AuthService {
  /// POST /api/auth/login
  /// Body attendu : { "email": "...", "password": "..." }
  /// Réponse : { "access_token": "...", "refresh_token": "..." }
  Future<AuthTokenResult> login({
    required String email,
    required String password,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/login'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': email, 'password': password}),
          )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return AuthTokenResult.ok(
          accessToken: data['access_token'] as String?,
          refreshToken: data['refresh_token'] as String?,
        );
      } else {
        return AuthTokenResult.fail(
          _extractErrorMessage(response.body) ?? 'Échec de connexion',
        );
      }
    } catch (_) {
      return AuthTokenResult.networkFailure();
    }
  }

  /// POST /api/auth/register
  /// Body attendu : { "firstname", "lastname", "email", "phoneNumber"
  ///                  (format international, ex: +22890000000),
  ///                  "countryCode" (ISO 3166-1 alpha-2, ex: "TG"), "password"
  ///                  (8+ caractères, 1 maj, 1 min, 1 chiffre) }
  /// Réponse : 202 Accepted, PAS de body. Un email de confirmation est envoyé ;
  /// l'utilisateur doit confirmer son compte avant de pouvoir se connecter.
  Future<AuthActionResult> register({
    required String firstname,
    required String lastname,
    required String email,
    required String phoneNumber,
    required String countryCode,
    required String password,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/register'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'firstname': firstname,
              'lastname': lastname,
              'email': email,
              'phoneNumber': phoneNumber,
              'countryCode': countryCode,
              'password': password,
            }),
          )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 202) {
        return AuthActionResult.ok();
      } else {
        return AuthActionResult.fail(
          _extractErrorMessage(response.body) ?? "Échec de l'inscription",
        );
      }
    } catch (e) {
      return AuthActionResult.fail(
        'Erreur réseau : impossible de contacter le serveur',
      );
    }
  }

  /// POST /api/auth/confirm
  /// Body attendu : { "token": "123456" }
  ///
  /// Était un GET avec le code en query param : une URL finit dans les logs
  /// serveur et l'historique, or ce code active le compte. Cf.
  /// ConfirmAccountRequest côté backend.
  Future<AuthActionResult> confirmAccount(String token) async {
    try {
      final response = await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/confirm'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'token': token}),
          )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        return AuthActionResult.ok();
      }
      return AuthActionResult.fail(
        _extractErrorMessage(response.body) ?? 'Échec de la confirmation',
      );
    } catch (e) {
      return AuthActionResult.fail(
        'Erreur réseau : impossible de contacter le serveur',
      );
    }
  }

  /// POST /api/auth/forgot-password
  /// Body attendu : { "email": "..." }
  Future<AuthActionResult> forgotPassword(String email) async {
    try {
      final response = await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/forgot-password'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'email': email}),
          )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 202) {
        return AuthActionResult.ok();
      }
      return AuthActionResult.fail(
        _extractErrorMessage(response.body) ?? "Échec de l'envoi",
      );
    } catch (e) {
      return AuthActionResult.fail(
        'Erreur réseau : impossible de contacter le serveur',
      );
    }
  }

  /// POST /api/auth/reset-password
  /// Body attendu : { "token": "123456", "newPassword": "..." }
  ///
  /// Les deux champs transitaient en query params : le mot de passe en clair
  /// et le code de réinitialisation étaient donc archivés dans les logs du
  /// serveur. Cf. ResetPasswordRequest côté backend.
  Future<AuthActionResult> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    try {
      final response = await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/reset-password'),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'token': token, 'newPassword': newPassword}),
          )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        return AuthActionResult.ok();
      }
      return AuthActionResult.fail(
        _extractErrorMessage(response.body) ?? 'Échec de la réinitialisation',
      );
    } catch (e) {
      return AuthActionResult.fail(
        'Erreur réseau : impossible de contacter le serveur',
      );
    }
  }

  /// POST /api/auth/refresh-token
  /// Header : `Authorization: Bearer <refresh_token>`
  Future<AuthTokenResult> refreshToken(String refreshToken) async {
    try {
      final response = await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/refresh-token'),
            headers: {'Authorization': 'Bearer $refreshToken'},
          )
          .timeout(AppConstants.apiTimeout);

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return AuthTokenResult.ok(
          accessToken: data['access_token'] as String?,
          refreshToken: data['refresh_token'] as String?,
        );
      }
      return AuthTokenResult.fail('Session expirée, veuillez vous reconnecter');
    } catch (_) {
      return AuthTokenResult.networkFailure();
    }
  }

  /// POST /api/auth/logout
  /// Header : `Authorization: Bearer <access_token>`
  /// Réponse : 204 No Content
  Future<void> logout(String accessToken) async {
    try {
      await http
          .post(
            Uri.parse('${AppConstants.baseUrl}/auth/logout'),
            headers: {'Authorization': 'Bearer $accessToken'},
          )
          .timeout(AppConstants.apiTimeout);
    } catch (_) {
      // Si le logout réseau échoue, on supprime quand même les tokens locaux
      // (cf. AuthProvider.logout) — l'important est que le frontend déconnecte.
    }
  }

  /// Extrait un message d'erreur lisible depuis le body JSON d'erreur Spring Boot.
  /// Gère le format Spring "MethodArgumentNotValidException" (liste de field errors)
  /// ainsi qu'un format simple { "message": "..." }.
  String? _extractErrorMessage(String body) {
    try {
      final data = jsonDecode(body);
      if (data is Map<String, dynamic>) {
        if (data['message'] != null) return data['message'] as String;
        if (data['errors'] is List && (data['errors'] as List).isNotEmpty) {
          final firstError = (data['errors'] as List).first;
          if (firstError is Map && firstError['defaultMessage'] != null) {
            return firstError['defaultMessage'] as String;
          }
        }
      }
    } catch (_) {
      // body non-JSON ou vide : on retombe sur le message par défaut de l'appelant.
    }
    return null;
  }
}
