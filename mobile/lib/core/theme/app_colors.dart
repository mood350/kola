import 'package:flutter/material.dart';

/// Palette KOLA — marine profond + jaune signature.
///
/// Les noms reprennent ceux du design system de référence (`theme.ts`) pour
/// qu'un écran porté depuis la maquette se relise sans table de correspondance.
class AppColors {
  AppColors._();

  // --- Fonds ---
  static const Color bg = Color(0xFFFAF8FF);
  static const Color white = Color(0xFFFFFFFF);

  // --- Texte ---
  static const Color ink = Color(0xFF131B2E);
  static const Color muted = Color(0xFF596171);

  // --- Marine (couleur de marque) ---
  static const Color primary = Color(0xFF002353);
  static const Color primary2 = Color(0xFF0F3875);
  static const Color blue = Color(0xFF0047BA);

  // --- Jaune signature ---
  static const Color yellow = Color(0xFFFFCB05);
  static const Color yellowDark = Color(0xFF745B00);

  // --- Vert (montants entrants, succès) ---
  static const Color green = Color(0xFF10B981);
  static const Color greenDark = Color(0xFF005236);
  static const Color mint = Color(0xFF6FFBBE);
  static const Color greenPale = Color(0xFFDDFBED);

  // --- Nuances froides ---
  static const Color pale = Color(0xFFF2F3FF);
  static const Color pale2 = Color(0xFFE2E7FF);
  static const Color border = Color(0xFFE2E8F0);
  static const Color hairline = Color(0xFFEDF0F5);

  // --- Erreur ---
  static const Color red = Color(0xFFBA1A1A);
  static const Color redPale = Color(0xFFFFDAD6);
  static const Color redDark = Color(0xFF93000A);

  /// Ombre douce teintée marine, appliquée aux cartes et aux boutons.
  static List<BoxShadow> get softShadow => const [
    BoxShadow(
      color: Color(0x1A0F3875),
      blurRadius: 10,
      offset: Offset(0, 3),
    ),
  ];
}
