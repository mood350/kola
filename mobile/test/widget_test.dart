import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/routes/app_routes.dart';

/// Remplace le test par défaut de l'app "compteur" Flutter (jamais adapté à
/// Kola, cassé depuis le début du projet). Ne pompe pas KolaApp/SplashScreen
/// directement : le splash a un Future.delayed non annulé + un appel à
/// StorageService (flutter_secure_storage), ce qui déclenche soit l'assertion
/// "Timer still pending" du test framework, soit un vrai appel de canal de
/// plateforme (voire FFI native sur Windows) — trop fragile pour un smoke test.
void main() {
  test('AppRoutes enregistre les routes principales de l\'application', () {
    expect(
      AppRoutes.routes.keys,
      containsAll(<String>[
        AppRoutes.splash,
        AppRoutes.onboarding,
        AppRoutes.login,
        AppRoutes.register,
        AppRoutes.forgotPassword,
        AppRoutes.home,
      ]),
    );
  });
}
