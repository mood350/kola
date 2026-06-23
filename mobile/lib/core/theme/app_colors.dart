import 'package:flutter/material.dart';

/// Palette de couleurs officielle de Kola.
/// Source : design system Stitch (kola_fintech/DESIGN.md)
class AppColors {
  AppColors._();

  // --- Surfaces ---
  static const Color surface = Color(0xFFF8F9FD);
  static const Color surfaceDim = Color(0xFFD8DADE);
  static const Color surfaceBright = Color(0xFFF8F9FD);
  static const Color surfaceContainerLowest = Color(0xFFFFFFFF);
  static const Color surfaceContainerLow = Color(0xFFF2F3F8);
  static const Color surfaceContainer = Color(0xFFECEEF2);
  static const Color surfaceContainerHigh = Color(0xFFE7E8EC);
  static const Color surfaceContainerHighest = Color(0xFFE1E2E6);
  static const Color surfaceCard = Color(0xFFFFFFFF);
  static const Color surfaceElevated = Color(0xFF16181A);
  static const Color surfaceVariant = Color(0xFFE1E2E6);

  // --- On-surface (texte) ---
  static const Color onSurface = Color(0xFF191C1F);
  static const Color onSurfaceVariant = Color(0xFF454555);
  static const Color inverseSurface = Color(0xFF2E3134);
  static const Color inverseOnSurface = Color(0xFFEFF1F5);

  // --- Outline ---
  static const Color outline = Color(0xFF767686);
  static const Color outlineVariant = Color(0xFFC6C5D7);

  // --- Primary (Cobalt Violet) ---
  static const Color primary = Color(0xFF2E32C7);
  static const Color onPrimary = Color(0xFFFFFFFF);
  static const Color primaryContainer = Color(0xFF494FDF);
  static const Color onPrimaryContainer = Color(0xFFDDDCFF);
  static const Color inversePrimary = Color(0xFFBFC1FF);
  static const Color primaryBright = Color(0xFF4F55F1);
  static const Color primaryDeep = Color(0xFF3A40C4);
  static const Color surfaceTint = Color(0xFF444ADB);

  static const Color primaryFixed = Color(0xFFE1E0FF);
  static const Color primaryFixedDim = Color(0xFFBFC1FF);
  static const Color onPrimaryFixed = Color(0xFF03006D);
  static const Color onPrimaryFixedVariant = Color(0xFF292CC3);

  // --- Secondary ---
  static const Color secondary = Color(0xFF5E5E5E);
  static const Color onSecondary = Color(0xFFFFFFFF);
  static const Color secondaryContainer = Color(0xFFE2E2E2);
  static const Color onSecondaryContainer = Color(0xFF646464);
  static const Color secondaryFixed = Color(0xFFE2E2E2);
  static const Color secondaryFixedDim = Color(0xFFC6C6C6);
  static const Color onSecondaryFixed = Color(0xFF1B1B1B);
  static const Color onSecondaryFixedVariant = Color(0xFF474747);

  // --- Tertiary ---
  static const Color tertiary = Color(0xFF494B4B);
  static const Color onTertiary = Color(0xFFFFFFFF);
  static const Color tertiaryContainer = Color(0xFF616363);
  static const Color onTertiaryContainer = Color(0xFFDFDFDF);
  static const Color tertiaryFixed = Color(0xFFE2E2E2);
  static const Color tertiaryFixedDim = Color(0xFFC6C6C7);
  static const Color onTertiaryFixed = Color(0xFF1A1C1C);
  static const Color onTertiaryFixedVariant = Color(0xFF454747);

  // --- Erreur ---
  static const Color error = Color(0xFFBA1A1A);
  static const Color onError = Color(0xFFFFFFFF);
  static const Color errorContainer = Color(0xFFFFDAD6);
  static const Color onErrorContainer = Color(0xFF93000A);

  // --- Background ---
  static const Color background = Color(0xFFF8F9FD);
  static const Color onBackground = Color(0xFF191C1F);

  // --- Sémantique fonctionnelle ---
  static const Color success = Color(0xFF00A87E);
  static const Color warning = Color(0xFFEC7E00);
  static const Color danger = Color(0xFFE23B4A);

  // --- Mobile Money (couleurs de marque) ---
  static const Color orangeMoney = Color(0xFFFF7900);
  static const Color mtnYellow = Color(0xFFFFCC00);
  static const Color waveBlue = Color(0xFF1DA1F2);
  static const Color moovBlue = Color(0xFF00539B);

  // --- Hairlines (bordures) ---
  static const Color hairlineLight = Color(0xFFE2E2E7);
  static const Color hairlineDarkOnDark = Color(0x1FFFFFFF); // blanc 12% opacité
}