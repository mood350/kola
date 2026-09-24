import '../models/user.dart';
import 'api_client.dart';

/// Issue d'un appel d'authentification.
///
/// Expose `accessToken` / `refreshToken` à plat parce que [SessionManager] ne
/// s'intéresse qu'à ça lors d'un renouvellement, sans avoir à déballer la
/// session complète — laquelle n'est de toute façon présente que pour une
/// connexion ou une inscription.
class AuthResult {
  const AuthResult({
    this.session,
    this.accessToken,
    this.refreshToken,
    this.error,
  });

  /// Connexion ou inscription : la réponse porte aussi le profil.
  factory AuthResult.fromSession(ApiResult<AuthSession> result) {
    final session = result.data;
    return AuthResult(
      session: session,
      accessToken: session?.accessToken,
      refreshToken: session?.refreshToken,
      error: result.error,
    );
  }

  factory AuthResult.ok({
    required String accessToken,
    String? refreshToken,
  }) => AuthResult(accessToken: accessToken, refreshToken: refreshToken);

  factory AuthResult.fail(String message) =>
      AuthResult(error: ApiException(code: 'AUTH_FAILED', message: message));

  factory AuthResult.networkFailure() => const AuthResult(
    error: ApiException(
      code: 'NETWORK_ERROR',
      message: 'Erreur réseau : impossible de contacter le serveur',
    ),
  );

  final AuthSession? session;
  final String? accessToken;
  final String? refreshToken;
  final ApiException? error;

  bool get success => accessToken != null;
  bool get isNetworkFailure => error?.isNetworkFailure ?? false;
  String get message => error?.message ?? '';
}

/// Délai d'attente avant de pouvoir redemander un code de vérification.
class OtpChallenge {
  const OtpChallenge({required this.phone, required this.expiresInSeconds});

  final String phone;
  final int expiresInSeconds;
}

class AuthService {
  AuthService({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  /// Étape 1 de l'inscription : envoi du code de vérification par SMS.
  Future<ApiResult<OtpChallenge>> requestOtp(String phone) async {
    final result = await _client.post(
      '/api/v1/auth/register/request-otp',
      body: {'phone': phone},
      decode: (json) {
        final map = (json as Map?)?.cast<String, dynamic>() ?? const {};
        return OtpChallenge(
          phone: map['phone']?.toString() ?? phone,
          expiresInSeconds:
              int.tryParse('${map['codeExpiresInSeconds']}') ?? 300,
        );
      },
    );
    return result;
  }

  /// Étape 2 : échange du code reçu contre un jeton de vérification, qui
  /// autorisera la création du compte.
  Future<ApiResult<String>> verifyOtp(String phone, String code) {
    return _client.post(
      '/api/v1/auth/register/verify-otp',
      body: {'phone': phone, 'code': code},
      decode: (json) =>
          (json as Map?)?['verificationToken']?.toString() ?? '',
    );
  }

  /// Étape 3 : création du compte et ouverture de session.
  Future<AuthResult> register({
    required String verificationToken,
    required String firstName,
    required String lastName,
    String? email,
    required String dateOfBirth,
    required String pin,
  }) async {
    final result = await _client.post(
      '/api/v1/auth/register',
      body: {
        'verificationToken': verificationToken,
        'firstName': firstName,
        'lastName': lastName,
        if (email != null && email.isNotEmpty) 'email': email,
        'dateOfBirth': dateOfBirth,
        'pin': pin,
        'confirmPin': pin,
        'acceptedPrivacyPolicy': true,
        'country': 'TG',
      },
      decode: (json) =>
          AuthSession.fromJson((json as Map).cast<String, dynamic>()),
    );
    return AuthResult.fromSession(result);
  }

  Future<AuthResult> login(String phone, String pin) async {
    final result = await _client.post(
      '/api/v1/auth/login',
      body: {'phone': phone, 'pin': pin},
      decode: (json) =>
          AuthSession.fromJson((json as Map).cast<String, dynamic>()),
    );
    return AuthResult.fromSession(result);
  }

  Future<AuthResult> refreshToken(String refreshToken) async {
    final result = await _client.post(
      '/api/v1/auth/refresh',
      body: {'refreshToken': refreshToken},
      decode: (json) =>
          AuthSession.fromJson((json as Map).cast<String, dynamic>()),
    );
    return AuthResult.fromSession(result);
  }

  Future<void> logout(String refreshToken) async {
    await _client.postEmpty(
      '/api/v1/auth/logout',
      body: {'refreshToken': refreshToken},
    );
  }
}
