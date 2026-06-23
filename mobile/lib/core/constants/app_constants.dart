/// Constantes globales de l'application Kola.
class AppConstants {
  AppConstants._();

  // --- API ---
  // ⚠️ À adapter selon l'environnement (dev local, staging, prod).
  // 10.0.2.2 = adresse spéciale pour accéder au localhost depuis l'émulateur Android.
  static const String baseUrl = 'http://10.0.2.2:8081/api';

  static const Duration apiTimeout = Duration(seconds: 15);

  // --- Stockage sécurisé (clés) ---
  static const String tokenKey = 'auth_token';
  static const String refreshTokenKey = 'refresh_token';

  // --- Devise ---
  static const String currencyCode = 'XOF';
  static const String currencySymbol = 'FCFA';

  // --- App ---
  static const String appName = 'Kola';
}