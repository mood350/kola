import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'core/constants/app_constants.dart';
import 'providers/auth_provider.dart';
import 'providers/beneficiary_provider.dart';
import 'providers/credit_provider.dart';
import 'providers/merchant_provider.dart';
import 'providers/notification_provider.dart';
import 'providers/payment_method_provider.dart';
import 'providers/scheduled_transfer_provider.dart';
import 'providers/transaction_provider.dart';
import 'providers/user_provider.dart';
import 'providers/vault_provider.dart';
import 'providers/wallet_provider.dart';
import 'routes/app_routes.dart';
import 'services/session_manager.dart';

/// Widget racine de l'application Kola.
class KolaApp extends StatelessWidget {
  const KolaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => WalletProvider()),
        ChangeNotifierProvider(create: (_) => UserProvider()),
        ChangeNotifierProvider(create: (_) => TransactionProvider()),
        ChangeNotifierProvider(create: (_) => VaultProvider()),
        ChangeNotifierProvider(create: (_) => CreditProvider()),
        ChangeNotifierProvider(create: (_) => BeneficiaryProvider()),
        ChangeNotifierProvider(create: (_) => MerchantProvider()),
        ChangeNotifierProvider(create: (_) => NotificationProvider()),
        ChangeNotifierProvider(create: (_) => ScheduledTransferProvider()),
        ChangeNotifierProvider(create: (_) => PaymentMethodProvider()),
      ],
      child: const _KolaMaterialApp(),
    );
  }
}

/// Séparé de [KolaApp] pour une raison précise : le branchement de
/// l'expiration de session a besoin de lire les providers, donc d'un contexte
/// situé SOUS le MultiProvider.
class _KolaMaterialApp extends StatefulWidget {
  const _KolaMaterialApp();

  @override
  State<_KolaMaterialApp> createState() => _KolaMaterialAppState();
}

class _KolaMaterialAppState extends State<_KolaMaterialApp> {
  @override
  void initState() {
    super.initState();
    SessionManager.instance.onSessionExpired = _handleSessionExpired;
  }

  @override
  void dispose() {
    SessionManager.instance.onSessionExpired = null;
    super.dispose();
  }

  /// Le refresh token est mort : la session ne peut plus être sauvée.
  ///
  /// Purger les providers n'est pas cosmétique — sans ça, l'écran de connexion
  /// s'affiche par-dessus des soldes et un historique appartenant au compte
  /// précédent, qui réapparaîtraient tels quels à la connexion suivante, même
  /// avec un autre utilisateur.
  void _handleSessionExpired() {
    if (!mounted) return;

    context.read<AuthProvider>().onSessionExpired();
    context.read<WalletProvider>().clear();
    context.read<UserProvider>().clear();
    context.read<TransactionProvider>().clear();
    context.read<VaultProvider>().clear();
    context.read<CreditProvider>().clear();
    context.read<BeneficiaryProvider>().clear();
    context.read<MerchantProvider>().clear();
    context.read<NotificationProvider>().clear();
    context.read<ScheduledTransferProvider>().clear();

    AppRoutes.navigatorKey.currentState?.pushNamedAndRemoveUntil(
      AppRoutes.login,
      (route) => false,
    );
    AppRoutes.scaffoldMessengerKey.currentState?.showSnackBar(
      const SnackBar(
        content: Text('Session expirée, veuillez vous reconnecter'),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: AppConstants.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      navigatorKey: AppRoutes.navigatorKey,
      scaffoldMessengerKey: AppRoutes.scaffoldMessengerKey,
      initialRoute: AppRoutes.splash,
      routes: AppRoutes.routes,
    );
  }
}
