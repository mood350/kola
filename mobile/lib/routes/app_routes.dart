import 'package:flutter/material.dart';
import '../screens/splash/splash_screen.dart';
import '../screens/onboarding/onboarding_screen.dart';
import '../screens/auth/login_screen.dart';
import '../screens/auth/register_screen.dart';
import '../screens/auth/forgot_password_screen.dart';
import '../screens/home/home_screen.dart';

/// Centralise toutes les routes nommées de l'application.
/// Navigation : Navigator.pushNamed(context, AppRoutes.login).
///
/// Note : pas de route OTP active — l'inscription est confirmée par lien
/// email (cf. AuthController.confirmAccount), pas par code à 6 chiffres.
/// L'écran otp_verification_screen.dart reste disponible dans le projet
/// pour un usage futur (2FA, vérification de transaction sensible...).
class AppRoutes {
  AppRoutes._();

  /// Permet de naviguer depuis en dehors de l'arbre de widgets. Nécessaire
  /// pour l'expiration de session : elle est détectée dans la couche réseau
  /// (SessionManager), qui n'a aucun BuildContext sous la main.
  static final GlobalKey<NavigatorState> navigatorKey =
      GlobalKey<NavigatorState>();

  /// Même raison, pour afficher le message « session expirée » : l'écran
  /// courant est démonté par la redirection, son ScaffoldMessenger avec.
  static final GlobalKey<ScaffoldMessengerState> scaffoldMessengerKey =
      GlobalKey<ScaffoldMessengerState>();

  static const String splash = '/';
  static const String onboarding = '/onboarding';
  static const String login = '/login';
  static const String register = '/register';
  static const String forgotPassword = '/forgot-password';
  static const String home = '/home';

  static Map<String, WidgetBuilder> routes = {
    splash: (context) => const SplashScreen(),
    onboarding: (context) => const OnboardingScreen(),
    login: (context) => const LoginScreen(),
    register: (context) => const RegisterScreen(),
    forgotPassword: (context) => const ForgotPasswordScreen(),
    home: (context) => const HomeScreen(),
  };
}
