import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'app_colors.dart';

/// Système typographique de Kola.
///
/// Remplacement des polices propriétaires par des équivalents Google Fonts :
/// - Aeonik Pro (headings, display) → Sora : même esprit architectural,
///   géométrique, avec un bon contraste de graisses.
/// - Inter (body, UI) → Inter (disponible nativement sur Google Fonts).
class AppTypography {
  AppTypography._();

  // --- Display & Headings (Sora, remplace Aeonik Pro) ---

  static TextStyle displayLg = GoogleFonts.sora(
    fontSize: 48,
    fontWeight: FontWeight.w500,
    height: 1.1,
    letterSpacing: -0.48,
    color: AppColors.onSurface,
  );

  static TextStyle displayLgMobile = GoogleFonts.sora(
    fontSize: 32,
    fontWeight: FontWeight.w500,
    height: 1.2,
    letterSpacing: -0.32,
    color: AppColors.onSurface,
  );

  static TextStyle headingMd = GoogleFonts.sora(
    fontSize: 24,
    fontWeight: FontWeight.w500,
    height: 1.33,
    letterSpacing: 0,
    color: AppColors.onSurface,
  );

  static TextStyle headingSm = GoogleFonts.sora(
    fontSize: 20,
    fontWeight: FontWeight.w500,
    height: 1.4,
    letterSpacing: 0,
    color: AppColors.onSurface,
  );

  // --- Body & UI (Inter) ---

  static TextStyle bodyLg = GoogleFonts.inter(
    fontSize: 18,
    fontWeight: FontWeight.w400,
    height: 1.56,
    letterSpacing: -0.09,
    color: AppColors.onSurface,
  );

  static TextStyle bodyMd = GoogleFonts.inter(
    fontSize: 16,
    fontWeight: FontWeight.w400,
    height: 1.5,
    letterSpacing: 0.24,
    color: AppColors.onSurfaceVariant,
  );

  static TextStyle bodyMdBold = GoogleFonts.inter(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.5,
    letterSpacing: 0.16,
    color: AppColors.onSurface,
  );

  static TextStyle bodySm = GoogleFonts.inter(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.43,
    letterSpacing: 0,
    color: AppColors.onSurfaceVariant,
  );

  static TextStyle buttonMd = GoogleFonts.inter(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.5,
    letterSpacing: 0.24,
    color: AppColors.onPrimary,
  );

  static TextStyle labelXs = GoogleFonts.inter(
    fontSize: 12,
    fontWeight: FontWeight.w600,
    height: 1.2,
    letterSpacing: 0.5,
    color: AppColors.onSurfaceVariant,
  );
}