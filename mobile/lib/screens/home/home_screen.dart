import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';

/// Écran d'accueil (placeholder temporaire).
/// Sera complété avec le détail complet (balance card, actions, transactions).
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      body: Center(
        child: Text('Accueil — à venir', style: AppTypography.headingMd),
      ),
    );
  }
}