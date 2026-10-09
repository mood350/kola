import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/constants/app_constants.dart';
import 'core/theme/app_colors.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/kola_shell.dart';
import 'models/user.dart';
import 'providers/kola_data_provider.dart';
import 'routes/kola_route.dart';
import 'screens/app_lock_screen.dart';
import 'screens/auth_screen.dart';
import 'screens/credit_screen.dart';
import 'screens/faq_screen.dart';
import 'screens/home_screen.dart';
import 'screens/kyc_screen.dart';
import 'screens/loan_detail_screen.dart';
import 'screens/loan_screen.dart';
import 'screens/onboarding_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/savings_screen.dart';
import 'screens/scan_screen.dart';
import 'screens/scheduled_screen.dart';
import 'screens/service_payments_screen.dart';
import 'screens/splash_screen.dart';
import 'screens/subscription_detail_screen.dart';
import 'screens/transaction_history_screen.dart';
import 'screens/vault_detail_screen.dart';
import 'screens/vaults_screen.dart';
import 'screens/widgets/assistant_widget.dart';
import 'screens/widgets/transaction_toast.dart';
import 'services/auth_service.dart';
import 'services/session_manager.dart';
import 'services/storage_service.dart';

/// Étapes d'ouverture de l'application.
enum _Entry { loading, onboarding, auth, lock, app }

class KolaApp extends StatelessWidget {
  const KolaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'KOLA',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      locale: const Locale('fr', 'FR'),
      supportedLocales: const [Locale('fr', 'FR')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: const _KolaRoot(),
    );
  }
}

class _KolaRoot extends StatefulWidget {
  const _KolaRoot();

  @override
  State<_KolaRoot> createState() => _KolaRootState();
}

class _KolaRootState extends State<_KolaRoot> with WidgetsBindingObserver {
  final _storage = StorageService();
  final _auth = AuthService();

  _Entry _entry = _Entry.loading;
  KolaUser? _user;
  DateTime? _backgroundedAt;

  KolaRoute _route = KolaRoute.home;
  String? _selectedVaultId;
  String? _selectedSubscriptionId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    SessionManager.instance.onSessionExpired = _onSessionExpired;
    _bootstrap();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    SessionManager.instance.onSessionExpired = null;
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive) {
      _backgroundedAt = DateTime.now();
      return;
    }
    if (state == AppLifecycleState.resumed && _entry == _Entry.app) {
      final since = _backgroundedAt;
      if (since != null &&
          DateTime.now().difference(since) > AppConstants.lockAfterBackground) {
        setState(() => _entry = _Entry.lock);
      }
    }
  }

  /// Décide de l'écran d'ouverture.
  ///
  /// Le délai minimal laisse le splash se jouer entièrement : sans lui, sur
  /// une session déjà en cache, l'écran apparaîtrait et disparaîtrait dans le
  /// même souffle.
  Future<void> _bootstrap() async {
    final started = DateTime.now();

    final preferences = await SharedPreferences.getInstance();
    final seenOnboarding =
        preferences.getBool(AppConstants.onboardingSeenKey) ?? false;
    final restored = await SessionManager.instance.restoreSession();
    final user = restored ? await _storage.readUser() : null;

    final elapsed = DateTime.now().difference(started);
    const minimum = Duration(milliseconds: 2600);
    if (elapsed < minimum) await Future<void>.delayed(minimum - elapsed);
    if (!mounted) return;

    setState(() {
      _user = user;
      if (user != null) {
        _entry = _Entry.lock;
      } else {
        _entry = seenOnboarding ? _Entry.auth : _Entry.onboarding;
      }
    });
  }

  Future<void> _finishOnboarding() async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setBool(AppConstants.onboardingSeenKey, true);
    if (mounted) setState(() => _entry = _Entry.auth);
  }

  Future<void> _authenticate(AuthSession session) async {
    await _persist(session);
    if (!mounted) return;
    setState(() {
      _user = session.user;
      _entry = _Entry.app;
    });
  }

  Future<void> _unlock(AuthSession? session) async {
    if (session != null) await _persist(session);
    _backgroundedAt = null;
    if (!mounted) return;
    setState(() {
      if (session != null) _user = session.user;
      _entry = _Entry.app;
    });
  }

  Future<void> _persist(AuthSession session) async {
    await _storage.saveToken(session.accessToken);
    await _storage.saveRefreshToken(session.refreshToken);
    await _storage.saveUser(session.user);
  }

  Future<void> _logout() async {
    final refreshToken = await _storage.getRefreshToken();
    if (refreshToken != null && refreshToken.isNotEmpty) {
      await _auth.logout(refreshToken);
    }
    await _storage.clearAll();
    if (!mounted) return;
    setState(() {
      _user = null;
      _route = KolaRoute.home;
      _entry = _Entry.auth;
    });
  }

  /// Le refresh token a expiré pendant l'utilisation : retour à la connexion
  /// sans passer par le verrouillage, qui exigerait une session valide.
  void _onSessionExpired() {
    if (!mounted) return;
    setState(() {
      _user = null;
      _route = KolaRoute.home;
      _entry = _Entry.auth;
    });
  }

  void _navigate(KolaRoute route) => setState(() => _route = route);

  void _openVault(String id) => setState(() {
    _selectedVaultId = id;
    _route = KolaRoute.vaultDetail;
  });

  void _openSubscription(String id) => setState(() {
    _selectedSubscriptionId = id;
    _route = KolaRoute.subscriptionDetail;
  });

  @override
  Widget build(BuildContext context) {
    final user = _user;

    return switch (_entry) {
      _Entry.loading => const SplashScreen(),
      _Entry.onboarding => OnboardingScreen(onFinish: _finishOnboarding),
      _Entry.auth => AuthScreen(onAuthenticated: _authenticate),
      _Entry.lock when user != null => AppLockScreen(
        user: user,
        onUnlock: _unlock,
        onLogout: _logout,
      ),
      // Une session verrouillée sans profil en cache n'a rien à déverrouiller.
      _Entry.lock => AuthScreen(onAuthenticated: _authenticate),
      _Entry.app => ChangeNotifierProvider(
        // La clé force un store neuf à chaque changement de compte, sinon le
        // nouvel utilisateur verrait un instant les soldes du précédent.
        key: ValueKey(user?.id ?? 'session'),
        create: (_) => KolaDataProvider()..start(),
        child: _AppShell(
          route: _route,
          onNavigate: _navigate,
          onOpenVault: _openVault,
          onOpenSubscription: _openSubscription,
          onLogout: _logout,
          selectedVaultId: _selectedVaultId,
          selectedSubscriptionId: _selectedSubscriptionId,
        ),
      ),
    };
  }
}

class _AppShell extends StatelessWidget {
  const _AppShell({
    required this.route,
    required this.onNavigate,
    required this.onOpenVault,
    required this.onOpenSubscription,
    required this.onLogout,
    required this.selectedVaultId,
    required this.selectedSubscriptionId,
  });

  final KolaRoute route;
  final void Function(KolaRoute) onNavigate;
  final void Function(String) onOpenVault;
  final void Function(String) onOpenSubscription;
  final Future<void> Function() onLogout;
  final String? selectedVaultId;
  final String? selectedSubscriptionId;

  @override
  Widget build(BuildContext context) {
    final toast = context.select<KolaDataProvider, TransactionToast?>(
      (data) => data.toast,
    );

    return Scaffold(
      backgroundColor: AppColors.bg,
      body: SafeArea(
        bottom: false,
        child: Stack(
          children: [
            Column(
              children: [
                AppHeader(onNavigate: onNavigate),
                Expanded(child: _screenFor(route)),
              ],
            ),
            const AssistantWidget(),
            if (toast != null)
              TransactionToastOverlay(
                toast: toast,
                onClose: context.read<KolaDataProvider>().dismissToast,
              ),
          ],
        ),
      ),
    );
  }

  Widget _screenFor(KolaRoute route) {
    return switch (route) {
      KolaRoute.home => HomeScreen(onNavigate: onNavigate),
      KolaRoute.savings => SavingsScreen(onNavigate: onNavigate),
      KolaRoute.vaults => VaultsScreen(
        onNavigate: onNavigate,
        onOpenVault: onOpenVault,
      ),
      KolaRoute.vaultDetail => VaultDetailScreen(
        onNavigate: onNavigate,
        vaultId: selectedVaultId,
      ),
      KolaRoute.scan => ScanScreen(onNavigate: onNavigate),
      KolaRoute.bills => ServicePaymentsScreen(
        onNavigate: onNavigate,
        subscriptions: false,
      ),
      KolaRoute.subscriptions => ServicePaymentsScreen(
        onNavigate: onNavigate,
        subscriptions: true,
        onOpenSubscription: onOpenSubscription,
      ),
      KolaRoute.subscriptionDetail => SubscriptionDetailScreen(
        onNavigate: onNavigate,
        subscriptionId: selectedSubscriptionId,
      ),
      KolaRoute.credit => CreditScreen(onNavigate: onNavigate),
      KolaRoute.loan => LoanScreen(onNavigate: onNavigate),
      KolaRoute.loanDetail => LoanDetailScreen(onNavigate: onNavigate),
      KolaRoute.profile => ProfileScreen(
        onNavigate: onNavigate,
        onLogout: onLogout,
      ),
      KolaRoute.faq => FaqScreen(onNavigate: onNavigate),
      KolaRoute.kyc => KycScreen(onNavigate: onNavigate),
      KolaRoute.transactionHistory => TransactionHistoryScreen(
        onNavigate: onNavigate,
      ),
      KolaRoute.scheduled => ScheduledScreen(onNavigate: onNavigate),
    };
  }
}
