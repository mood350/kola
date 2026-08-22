import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../providers/auth_provider.dart';
import '../../routes/app_routes.dart';

/// Écran de démarrage (Splash Screen).
/// Affiche le logo Kola avec une entrée animée (fondu + zoom léger) puis
/// redirige automatiquement après un court délai.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _logoScale;
  late final Animation<double> _logoOpacity;

  @override
  void initState() {
    super.initState();

    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    );
    _logoScale = Tween<double>(
      begin: 0.85,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeOutBack));
    _logoOpacity = CurvedAnimation(parent: _controller, curve: Curves.easeOut);
    _controller.forward();

    _navigateNext();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  /// Le splash lisait le stockage lui-même pendant qu'AuthProvider gardait sa
  /// propre version de la même décision, jamais appelée : deux chemins pour
  /// une seule question, voués à diverger. Un seul subsiste, et il sait en
  /// plus renouveler un access token expiré au lieu de renvoyer sur
  /// l'onboarding un utilisateur dont la session est parfaitement valide.
  Future<void> _navigateNext() async {
    final authProvider = context.read<AuthProvider>();
    // La restauration (et l'éventuel appel réseau de renouvellement) tourne
    // pendant l'animation plutôt qu'après : le délai est masqué.
    final restored = authProvider.checkAuthStatus();
    await Future.wait([
      restored,
      Future<void>.delayed(const Duration(milliseconds: 1800)),
    ]);
    if (!mounted) return;

    Navigator.pushReplacementNamed(
      context,
      authProvider.isAuthenticated ? AppRoutes.home : AppRoutes.onboarding,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            FadeTransition(
              opacity: _logoOpacity,
              child: ScaleTransition(
                scale: _logoScale,
                child: Container(
                  width: 96,
                  height: 96,
                  decoration: BoxDecoration(
                    color: AppColors.primaryContainer,
                    borderRadius: BorderRadius.circular(AppRadius.xl),
                  ),
                  child: const Icon(
                    Icons.bolt_rounded,
                    color: Colors.white,
                    size: 48,
                  ),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            FadeTransition(
              opacity: _logoOpacity,
              child: Text(
                'Kola',
                style: AppTypography.displayLgMobile.copyWith(
                  color: AppColors.primaryContainer,
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
            const _LoadingDots(),
          ],
        ),
      ),
    );
  }
}

/// Trois petits points animés (équivalent du "..." de la maquette), chacun
/// piloté par sa propre [CurvedAnimation] avec un [Interval] décalé plutôt
/// qu'un calcul d'opacité manuel.
class _LoadingDots extends StatefulWidget {
  const _LoadingDots();

  @override
  State<_LoadingDots> createState() => _LoadingDotsState();
}

class _LoadingDotsState extends State<_LoadingDots>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final List<Animation<double>> _dotOpacities;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat();

    _dotOpacities = List.generate(3, (index) {
      final start = index * 0.2;
      final end = (start + 0.6).clamp(0.0, 1.0);
      return TweenSequence<double>([
        TweenSequenceItem(tween: Tween(begin: 0.3, end: 1.0), weight: 1),
        TweenSequenceItem(tween: Tween(begin: 1.0, end: 0.3), weight: 1),
      ]).animate(
        CurvedAnimation(
          parent: _controller,
          curve: Interval(start, end, curve: Curves.easeInOut),
        ),
      );
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) {
        return Row(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(3, (index) {
            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 3),
              child: Opacity(
                opacity: _dotOpacities[index].value,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: AppColors.primaryContainer,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            );
          }),
        );
      },
    );
  }
}
