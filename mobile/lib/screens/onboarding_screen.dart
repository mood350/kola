import 'package:flutter/material.dart';
import '../core/theme/kola_icons.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';

class _Slide {
  const _Slide(this.image, this.eyebrow, this.title, this.text);
  final String image;
  final String eyebrow;
  final String title;
  final String text;
}

const _slides = <_Slide>[
  _Slide(
    'assets/onboarding/financial-challenges.png',
    'UNE RÉALITÉ QUOTIDIENNE',
    'Votre argent mérite mieux que la dispersion',
    'Paiements, espèces, factures et épargne sont difficiles à suivre quand tout est séparé.',
  ),
  _Slide(
    'assets/onboarding/connected-network.png',
    'UN SEUL RÉSEAU',
    'Payez et recevez simplement',
    'KOLA réunit particuliers, marchands et Mobile Money dans une expérience sécurisée.',
  ),
  _Slide(
    'assets/onboarding/smart-savings.png',
    'VOS OBJECTIFS',
    'Épargnez à votre rythme',
    'Créez des coffres, programmez vos versements et avancez vers chaque projet.',
  ),
  _Slide(
    'assets/onboarding/fair-credit.png',
    'VOTRE PROGRESSION',
    'Construisez votre accès au crédit',
    'Vos bonnes habitudes renforcent votre score et ouvrent des offres adaptées.',
  ),
];

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key, required this.onFinish});

  final VoidCallback onFinish;

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _controller = PageController();
  int _index = 0;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _next() {
    if (_index == _slides.length - 1) {
      widget.onFinish();
      return;
    }
    _controller.nextPage(
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  Widget build(BuildContext context) {
    final last = _index == _slides.length - 1;
    final imageHeight = (MediaQuery.of(context).size.height * 0.48).clamp(
      0.0,
      430.0,
    );

    return Scaffold(
      backgroundColor: AppColors.bg,
      body: SafeArea(
        child: Column(
          children: [
            SizedBox(
              height: 58,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.gutter,
                ),
                child: Row(
                  children: [
                    Image.asset('assets/kola-logo-transparent.png', width: 70),
                    const Spacer(),
                    GestureDetector(
                      onTap: widget.onFinish,
                      child: Text(
                        'Passer',
                        style: AppTypography.smallBold.copyWith(
                          color: AppColors.muted,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Expanded(
              child: PageView.builder(
                controller: _controller,
                itemCount: _slides.length,
                onPageChanged: (value) => setState(() => _index = value),
                itemBuilder: (context, i) {
                  final slide = _slides[i];
                  return Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 27),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        SizedBox(
                          height: imageHeight,
                          child: Image.asset(slide.image, fit: BoxFit.contain),
                        ),
                        const SizedBox(height: 7),
                        Text(
                          slide.eyebrow,
                          style: AppTypography.badge.copyWith(
                            fontWeight: FontWeight.w900,
                            letterSpacing: 1.1,
                            color: AppColors.greenDark,
                          ),
                        ),
                        const SizedBox(height: AppSpacing.xs),
                        Text(
                          slide.title,
                          textAlign: TextAlign.center,
                          style: AppTypography.screenTitle.copyWith(
                            fontSize: 25,
                            height: 1.28,
                          ),
                        ),
                        const SizedBox(height: 9),
                        Text(
                          slide.text,
                          textAlign: TextAlign.center,
                          style: AppTypography.small.copyWith(
                            fontSize: 12,
                            height: 1.58,
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                AppSpacing.gutter,
                0,
                AppSpacing.gutter,
                18,
              ),
              child: Column(
                children: [
                  SizedBox(
                    height: 26,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        for (var i = 0; i < _slides.length; i++)
                          AnimatedContainer(
                            duration: const Duration(milliseconds: 220),
                            margin: const EdgeInsets.symmetric(horizontal: 3),
                            width: i == _index ? 24 : 7,
                            height: 7,
                            decoration: BoxDecoration(
                              color: i == _index
                                  ? AppColors.primary
                                  : AppColors.pale2,
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  GestureDetector(
                    onTap: _next,
                    child: Container(
                      height: 54,
                      decoration: BoxDecoration(
                        color: AppColors.yellow,
                        borderRadius: BorderRadius.circular(27),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            last ? 'Commencer' : 'Continuer',
                            style: AppTypography.button.copyWith(
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(width: 9),
                          Icon(
                            last ? KolaIcons.checkmark : KolaIcons.arrowForward,
                            size: 20,
                            color: AppColors.primary,
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
