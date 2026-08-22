import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/widgets/primary_button.dart';
import '../../routes/app_routes.dart';

class _OnboardingSlide {
  final IconData icon;
  final String title;
  final String description;

  const _OnboardingSlide({
    required this.icon,
    required this.title,
    required this.description,
  });
}

const _slides = [
  _OnboardingSlide(
    icon: Icons.bolt_rounded,
    title: "Payez en un clin d'œil",
    description:
        "Envoyez et recevez de l'argent instantanément, sans frais cachés ni complications.",
  ),
  _OnboardingSlide(
    icon: Icons.lock_outline_rounded,
    title: 'Épargnez pour demain',
    description:
        'Créez des coffres-forts dédiés à vos projets et regardez votre argent fructifier en toute sécurité.',
  ),
  _OnboardingSlide(
    icon: Icons.show_chart_rounded,
    title: 'Votre activité est votre garantie',
    description:
        'Construisez votre score de crédit simplement en utilisant l\'application pour vos transactions quotidiennes.',
  ),
];

/// Écran d'onboarding : carrousel de 3 slides présentant les piliers de Kola
/// (wallet, vaults, crédit), avec pagination par points et bouton Suivant/Commencer.
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentIndex = 0;

  bool get _isLastSlide => _currentIndex == _slides.length - 1;

  void _onNext() {
    if (_isLastSlide) {
      Navigator.pushReplacementNamed(context, AppRoutes.login);
    } else {
      _pageController.nextPage(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    }
  }

  void _onSkip() {
    Navigator.pushReplacementNamed(context, AppRoutes.login);
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                itemCount: _slides.length,
                onPageChanged: (index) => setState(() => _currentIndex = index),
                itemBuilder: (context, index) {
                  final slide = _slides[index];
                  return Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.marginMobile,
                    ),
                    child: Column(
                      children: [
                        const SizedBox(height: AppSpacing.xl),
                        // Illustration (remplacée par une icône stylisée dans un container)
                        // Échelle + fondu selon la distance à la page active,
                        // pour un effet de profondeur pendant le swipe.
                        AnimatedBuilder(
                          animation: _pageController,
                          builder: (context, child) {
                            double distance = 0;
                            if (_pageController.hasClients &&
                                _pageController.position.haveDimensions) {
                              distance =
                                  ((_pageController.page ??
                                              _currentIndex.toDouble()) -
                                          index)
                                      .abs()
                                      .clamp(0.0, 1.0);
                            } else if (index != _currentIndex) {
                              distance = 1;
                            }
                            final scale = 1 - (distance * 0.15);
                            final opacity = 1 - (distance * 0.5);
                            return Opacity(
                              opacity: opacity,
                              child: Transform.scale(
                                scale: scale,
                                child: child,
                              ),
                            );
                          },
                          child: Container(
                            width: double.infinity,
                            constraints: const BoxConstraints(maxWidth: 320),
                            height: 320,
                            decoration: BoxDecoration(
                              color: AppColors.surfaceContainerLow,
                              borderRadius: BorderRadius.circular(32),
                            ),
                            child: Center(
                              child: Container(
                                width: 96,
                                height: 96,
                                decoration: BoxDecoration(
                                  color: AppColors.primaryContainer,
                                  borderRadius: BorderRadius.circular(
                                    AppRadius.xl,
                                  ),
                                ),
                                child: Icon(
                                  slide.icon,
                                  color: Colors.white,
                                  size: 48,
                                ),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: AppSpacing.xl),
                        Text(
                          slide.title,
                          textAlign: TextAlign.center,
                          style: AppTypography.displayLgMobile,
                        ),
                        const SizedBox(height: AppSpacing.md),
                        Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.md,
                          ),
                          child: Text(
                            slide.description,
                            textAlign: TextAlign.center,
                            style: AppTypography.bodyLg.copyWith(
                              color: AppColors.onSurfaceVariant,
                            ),
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            // Dots de pagination
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(_slides.length, (index) {
                final isActive = index == _currentIndex;
                return AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  margin: const EdgeInsets.symmetric(horizontal: 4),
                  width: isActive ? 24 : 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: isActive
                        ? AppColors.primary
                        : AppColors.surfaceVariant,
                    borderRadius: BorderRadius.circular(AppRadius.full),
                  ),
                );
              }),
            ),
            const SizedBox(height: AppSpacing.lg),
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.marginMobile,
              ),
              child: Column(
                children: [
                  PrimaryButton(
                    label: _isLastSlide ? 'Commencer' : 'Suivant',
                    onPressed: _onNext,
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  TextButton(
                    onPressed: _onSkip,
                    child: Text(
                      'Passer',
                      style: AppTypography.bodyMd.copyWith(
                        color: AppColors.onSurfaceVariant,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
          ],
        ),
      ),
    );
  }
}
