import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'app_colors.dart';

/// Échelle typographique KOLA.
///
/// Elle est volontairement dense et graissée : la maquette empile beaucoup
/// d'informations chiffrées par écran, et hiérarchise par le poids plutôt que
/// par la taille. Inter couvre l'ensemble, chiffres compris.
class AppTypography {
  AppTypography._();

  static TextStyle _base(
    double size,
    FontWeight weight, {
    Color color = AppColors.ink,
    double? height,
    double? letterSpacing,
  }) {
    return GoogleFonts.inter(
      fontSize: size,
      fontWeight: weight,
      color: color,
      height: height,
      letterSpacing: letterSpacing,
    );
  }

  /// Solde du portefeuille, montant d'un reçu.
  static TextStyle get balance =>
      _base(31, FontWeight.w900, color: AppColors.yellow);

  /// Titre d'une feuille modale ou d'un écran plein.
  static TextStyle get screenTitle =>
      _base(22, FontWeight.w900, color: AppColors.ink);

  /// Intitulé d'une section ("Actions Express", "Mes Coffres-Forts").
  static TextStyle get section =>
      _base(20, FontWeight.w800, color: AppColors.ink, letterSpacing: -0.3);

  /// Montant mis en avant dans une carte.
  static TextStyle get amount =>
      _base(19, FontWeight.w800, color: AppColors.primary);

  static TextStyle get cardTitle =>
      _base(15, FontWeight.w800, color: AppColors.ink);

  static TextStyle get body => _base(14, FontWeight.w400, color: AppColors.ink);

  static TextStyle get bodyBold =>
      _base(14, FontWeight.w700, color: AppColors.ink);

  static TextStyle get button =>
      _base(14, FontWeight.w800, color: AppColors.primary);

  static TextStyle get small =>
      _base(11, FontWeight.w400, color: AppColors.muted);

  static TextStyle get smallBold =>
      _base(11, FontWeight.w700, color: AppColors.primary);

  /// Légende, horodatage, mention sous un montant.
  static TextStyle get caption =>
      _base(10, FontWeight.w400, color: AppColors.muted, height: 1.4);

  /// Intitulé de champ de formulaire, en capitales.
  static TextStyle get fieldLabel =>
      _base(9, FontWeight.w800, color: AppColors.muted);

  /// Texte d'un badge ou d'une pastille.
  static TextStyle get badge =>
      _base(9, FontWeight.w700, color: AppColors.primary);
}
