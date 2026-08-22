/// Constantes globales de l'application Kola.
class AppConstants {
  AppConstants._();

  // --- API ---

  /// URL de base de l'API, surchargeable au build sans toucher au code :
  ///
  ///   flutter run  --dart-define=KOLA_API_BASE_URL=https://api.kola.tg/api
  ///   flutter build apk --dart-define=KOLA_API_BASE_URL=https://api.kola.tg/api
  ///
  /// La valeur par défaut ne vaut QUE pour l'émulateur Android : 10.0.2.2 est
  /// l'alias par lequel l'émulateur atteint le localhost de la machine hôte.
  /// Ni le simulateur iOS, ni un device physique, ni un backend distant ne
  /// connaissent cette adresse — d'où la surcharge.
  ///
  /// `--dart-define` est résolu à la compilation : la valeur est figée dans le
  /// binaire, il n'y a rien à lire au démarrage et rien à embarquer dans un
  /// fichier de config livré avec l'app.
  ///
  /// ⚠️ Un build de production doit viser du https. Le trafic en clair n'est
  /// autorisé que par le manifeste de la variante debug
  /// (android/app/src/debug/AndroidManifest.xml) : une release qui pointerait
  /// sur du http échouerait à émettre, par construction.
  static const String baseUrl = String.fromEnvironment(
    'KOLA_API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8081/api',
  );

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
