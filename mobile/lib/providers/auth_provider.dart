import 'package:flutter/foundation.dart';
import '../services/auth_service.dart';
import '../services/session_manager.dart';
import '../services/storage_service.dart';

enum AuthStatus { initial, loading, authenticated, unauthenticated, error }

/// Provider gérant l'état d'authentification de l'utilisateur.
/// Utilisé via `Provider.of<AuthProvider>(context)` ou `context.watch<AuthProvider>()`.
///
/// ⚠️ Important : le backend Kola exige une confirmation par email après
/// l'inscription (POST /auth/register renvoie 202 sans tokens). L'utilisateur
/// ne peut se connecter qu'après avoir saisi le code à 6 chiffres reçu par
/// email (cf. confirmAccount / OtpVerificationScreen).
class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();
  final StorageService _storageService = StorageService();

  AuthStatus _status = AuthStatus.initial;
  String? _errorMessage;
  String? _accessToken;

  AuthStatus get status => _status;
  String? get errorMessage => _errorMessage;
  bool get isAuthenticated => _status == AuthStatus.authenticated;

  /// Restaure la session au démarrage de l'app.
  ///
  /// Se contentait de constater la présence d'un token, sans regarder sa date
  /// d'expiration : un access token périmé passait donc pour valide. La
  /// décision revient maintenant à [SessionManager], qui vérifie l'expiration
  /// et, si besoin, renouvelle silencieusement à partir du refresh token —
  /// une session de plus de 24 h n'oblige plus à ressaisir son mot de passe.
  Future<void> checkAuthStatus() async {
    final restored = await SessionManager.instance.restoreSession();
    _accessToken = restored ? await _storageService.getToken() : null;
    _status = restored ? AuthStatus.authenticated : AuthStatus.unauthenticated;
    notifyListeners();
  }

  /// Connexion par email + mot de passe (PAS de téléphone — le backend
  /// n'accepte que l'email pour le login, cf. AuthenticationRequest).
  Future<bool> login(String email, String password) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    final result = await _authService.login(email: email, password: password);

    if (result.success) {
      _accessToken = result.accessToken;
      await _storageService.saveToken(result.accessToken!);
      if (result.refreshToken != null) {
        await _storageService.saveRefreshToken(result.refreshToken!);
      }
      _status = AuthStatus.authenticated;
      notifyListeners();
      return true;
    } else {
      _errorMessage = result.errorMessage;
      _status = AuthStatus.error;
      notifyListeners();
      return false;
    }
  }

  /// Inscription. Ne connecte PAS automatiquement l'utilisateur :
  /// le backend envoie un email de confirmation à valider avant le premier login.
  /// Retourne true si la demande a été acceptée (202), pas si le compte est actif.
  Future<bool> register({
    required String firstname,
    required String lastname,
    required String email,
    required String phoneNumber,
    required String countryCode,
    required String password,
  }) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    final result = await _authService.register(
      firstname: firstname,
      lastname: lastname,
      email: email,
      phoneNumber: phoneNumber,
      countryCode: countryCode,
      password: password,
    );

    if (result.success) {
      _status =
          AuthStatus.unauthenticated; // compte créé mais pas encore confirmé
      notifyListeners();
      return true;
    } else {
      _errorMessage = result.errorMessage;
      _status = AuthStatus.error;
      notifyListeners();
      return false;
    }
  }

  /// Active le compte avec le code à 6 chiffres reçu par email.
  /// Ne connecte pas l'utilisateur : le backend ne renvoie aucun token ici,
  /// il faut passer par login ensuite.
  Future<bool> confirmAccount(String code) async {
    _errorMessage = null;
    final result = await _authService.confirmAccount(code);
    if (!result.success) {
      _errorMessage = result.errorMessage;
      notifyListeners();
    }
    return result.success;
  }

  Future<bool> forgotPassword(String email) async {
    final result = await _authService.forgotPassword(email);
    if (!result.success) {
      _errorMessage = result.errorMessage;
      notifyListeners();
    }
    return result.success;
  }

  /// Pose un nouveau mot de passe à partir du code reçu par email.
  /// Le code est validé par le backend au moment de cet appel — il n'existe
  /// pas de route qui le vérifie seul (cf. ResetPasswordRequest).
  Future<bool> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    _errorMessage = null;
    final result = await _authService.resetPassword(
      token: token,
      newPassword: newPassword,
    );
    if (!result.success) {
      _errorMessage = result.errorMessage;
      notifyListeners();
    }
    return result.success;
  }

  /// Session invalidée par le backend (refresh token expiré ou révoqué).
  /// Les tokens sont déjà purgés par [SessionManager] : il ne reste qu'à
  /// remettre l'état mémoire à zéro. Surtout pas d'appel réseau à
  /// /auth/logout ici — il repartirait avec le token que le serveur vient de
  /// rejeter.
  void onSessionExpired() {
    _accessToken = null;
    _errorMessage = 'Session expirée, veuillez vous reconnecter';
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }

  Future<void> logout() async {
    // Au redémarrage de l'app, la session est restaurée par SplashScreen
    // directement depuis le stockage : _accessToken reste null alors que
    // l'utilisateur est bien connecté. S'en tenir au champ en mémoire faisait
    // donc sauter l'appel serveur pour toute session restaurée — on retombe
    // sur le stockage, seule source de vérité du token.
    final token = _accessToken ?? await _storageService.getToken();
    if (token != null && token.isNotEmpty) {
      await _authService.logout(token);
    }
    await _storageService.clearAll();
    _accessToken = null;
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }
}
