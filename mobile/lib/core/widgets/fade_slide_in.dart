import 'package:flutter/material.dart';

/// Anime l'apparition d'un enfant (fondu + léger glissement vers le haut),
/// avec un délai optionnel pour un effet "cascade" quand utilisé sur des
/// éléments de liste (delay = index * staggerStep).
class FadeSlideIn extends StatelessWidget {
  final Widget child;
  final Duration delay;
  final Duration duration;

  const FadeSlideIn({
    super.key,
    required this.child,
    this.delay = Duration.zero,
    this.duration = const Duration(milliseconds: 350),
  });

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: duration + delay,
      curve: Interval(_delayFraction, 1.0, curve: Curves.easeOutCubic),
      builder: (context, value, child) {
        return Opacity(
          opacity: value,
          child: Transform.translate(
            offset: Offset(0, (1 - value) * 16),
            child: child,
          ),
        );
      },
      child: child,
    );
  }

  double get _delayFraction {
    final total = (duration + delay).inMilliseconds;
    if (total == 0) return 0;
    return (delay.inMilliseconds / total).clamp(0.0, 0.9);
  }
}
