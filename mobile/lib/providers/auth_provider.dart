import 'package:flutter/foundation.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/storage_service.dart';

enum AuthStatus { initial, loading, authenticated, unauthenticated, error }

/// Provider gérant l'état d'authentification de l'utilisateur.
/// Utilisé via `Provider.of<AuthProvider>(context)` ou `context.watch<AuthProvider>()`.
class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();
  final StorageService _storageService = StorageService();

  AuthStatus _status = AuthStatus.initial;
  User? _currentUser;
  String? _errorMessage;

  AuthStatus get status => _status;
  User? get currentUser => _currentUser;
  String? get errorMessage => _errorMessage;
  bool get isAuthenticated => _status == AuthStatus.authenticated;

  /// Vérifie si un token existe déjà au démarrage de l'app (auto-login).
  Future<void> checkAuthStatus() async {
    final token = await _storageService.getToken();
    if (token != null && token.isNotEmpty) {
      // TODO: idéalement, valider le token auprès du backend (endpoint /auth/me)
      _status = AuthStatus.authenticated;
    } else {
      _status = AuthStatus.unauthenticated;
    }
    notifyListeners();
  }

  Future<bool> login(String phoneNumber, String password) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    final result = await _authService.login(
      phoneNumber: phoneNumber,
      password: password,
    );

    if (result.success) {
      await _storageService.saveToken(result.token!);
      if (result.refreshToken != null) {
        await _storageService.saveRefreshToken(result.refreshToken!);
      }
      _currentUser = result.user;
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

  Future<bool> register(String fullName, String phoneNumber, String password) async {
    _status = AuthStatus.loading;
    _errorMessage = null;
    notifyListeners();

    final result = await _authService.register(
      fullName: fullName,
      phoneNumber: phoneNumber,
      password: password,
    );

    if (result.success) {
      await _storageService.saveToken(result.token!);
      if (result.refreshToken != null) {
        await _storageService.saveRefreshToken(result.refreshToken!);
      }
      _currentUser = result.user;
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

  Future<void> logout() async {
    await _storageService.clearAll();
    _currentUser = null;
    _status = AuthStatus.unauthenticated;
    notifyListeners();
  }
}