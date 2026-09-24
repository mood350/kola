import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_typography.dart';

/// Écran d'ouverture : le logo apparaît, le titre le suit, et une pièce
/// traverse la carte en boucle pour illustrer l'envoi instantané.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with TickerProviderStateMixin {
  late final AnimationController _intro = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 950),
  )..forward();

  late final AnimationController _coin = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1700),
  )..repeat();

  late final Animation<double> _logoOpacity = CurvedAnimation(
    parent: _intro,
    curve: const Interval(0, 0.55, curve: Curves.easeOut),
  );

  late final Animation<double> _logoScale = Tween<double>(
    begin: 0.55,
    end: 1,
  ).animate(
    CurvedAnimation(
      parent: _intro,
      curve: const Interval(0, 0.55, curve: Curves.elasticOut),
    ),
  );

  late final Animation<double> _titleOpacity = CurvedAnimation(
    parent: _intro,
    curve: const Interval(0.5, 1, curve: Curves.easeOut),
  );

  late final Animation<double> _titleOffset = Tween<double>(
    begin: 18,
    end: 0,
  ).animate(
    CurvedAnimation(
      parent: _intro,
      curve: const Interval(0.5, 1, curve: Curves.easeOutCubic),
    ),
  );

  @override
  void dispose() {
    _intro.dispose();
    _coin.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      color: AppColors.primary,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              FadeTransition(
                opacity: _logoOpacity,
                child: ScaleTransition(
                  scale: _logoScale,
                  child: Image.asset(
                    'assets/kola-logo-transparent.png',
                    width: 150,
                    height: 150,
                    fit: BoxFit.contain,
                  ),
                ),
              ),
              AnimatedBuilder(
                animation: _intro,
                builder: (context, child) => Opacity(
                  opacity: _titleOpacity.value,
                  child: Transform.translate(
                    offset: Offset(0, _titleOffset.value),
                    child: child,
                  ),
                ),
                child: Column(
                  children: [
                    const SizedBox(height: 5),
                    Text(
                      'KOLA',
                      style: AppTypography.screenTitle.copyWith(
                        fontSize: 34,
                        letterSpacing: 5,
                        color: AppColors.white,
                      ),
                    ),
                    const SizedBox(height: 7),
                    Text(
                      'Votre finance. Votre avenir.',
                      style: AppTypography.small.copyWith(
                        letterSpacing: 0.8,
                        color: const Color(0xFFD8E2FF),
                      ),
                    ),
                    const SizedBox(height: 28),
                    FadeTransition(
                      opacity: _titleOpacity,
                      child: _TransferCard(progress: _coin),
                    ),
                  ],
                ),
              ),
            ],
          ),
          Positioned(
            bottom: 55,
            child: FadeTransition(
              opacity: _titleOpacity,
              child: Container(
                width: 84,
                height: 3,
                decoration: BoxDecoration(
                  color: AppColors.white.withValues(alpha: 0.13),
                  borderRadius: BorderRadius.circular(2),
                ),
                child: FractionallySizedBox(
                  alignment: Alignment.centerLeft,
                  widthFactor: 0.72,
                  child: Container(
                    decoration: BoxDecoration(
                      color: AppColors.yellow,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TransferCard extends StatelessWidget {
  const _TransferCard({required this.progress});

  final Animation<double> progress;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 240,
      height: 90,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Container(
            width: 240,
            height: 72,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            decoration: BoxDecoration(
              color: AppColors.white.withValues(alpha: 0.07),
              borderRadius: BorderRadius.circular(22),
              border: Border.all(
                color: AppColors.white.withValues(alpha: 0.13),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const _Endpoint(KolaIcons.wallet, AppColors.yellow),
                SizedBox(
                  width: 104,
                  height: 42,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      const Icon(
                        KolaIcons.arrowForward,
                        size: 18,
                        color: Color(0xFFADC6FF),
                      ),
                      AnimatedBuilder(
                        animation: progress,
                        builder: (context, child) {
                          // La pièce parcourt la ligne sur les deux premiers
                          // tiers du cycle, puis marque une pause : c'est ce
                          // temps mort qui donne la cadence d'un envoi.
                          final t = (progress.value / 0.65).clamp(0.0, 1.0);
                          final visible = progress.value < 0.65;
                          return Transform.translate(
                            offset: Offset(-54 + 108 * t, 0),
                            child: Opacity(
                              opacity: visible ? 1 : 0,
                              child: child,
                            ),
                          );
                        },
                        child: Container(
                          width: 28,
                          height: 28,
                          alignment: Alignment.center,
                          decoration: const BoxDecoration(
                            color: AppColors.yellow,
                            shape: BoxShape.circle,
                          ),
                          child: Text(
                            'F',
                            style: AppTypography.badge.copyWith(
                              fontSize: 13,
                              fontWeight: FontWeight.w900,
                              color: AppColors.primary,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const _Endpoint(KolaIcons.person, AppColors.white),
              ],
            ),
          ),
          Positioned(
            left: 0,
            right: 0,
            bottom: 0,
            child: Text(
              'ENVOI INSTANTANÉ',
              textAlign: TextAlign.center,
              style: AppTypography.badge.copyWith(
                fontSize: 8,
                fontWeight: FontWeight.w800,
                letterSpacing: 1.5,
                color: const Color(0xFFADC6FF),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Endpoint extends StatelessWidget {
  const _Endpoint(this.icon, this.color);

  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 42,
      height: 42,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppColors.white.withValues(alpha: 0.09),
        borderRadius: BorderRadius.circular(13),
      ),
      child: Icon(icon, size: 22, color: color),
    );
  }
}
