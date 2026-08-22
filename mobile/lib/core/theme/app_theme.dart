import 'package:animations/animations.dart';
import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'app_typography.dart';
import 'app_spacing.dart';

/// Transition Material "shared axis" (glissement horizontal + fondu) —
/// remplace le slide iOS/Android par défaut pour un système de transitions
/// cohérent sur toutes les plateformes et tous les `Navigator.push`.
class _SharedAxisPageTransitionsBuilder extends PageTransitionsBuilder {
  const _SharedAxisPageTransitionsBuilder();

  @override
  Widget buildTransitions<T>(
    PageRoute<T> route,
    BuildContext context,
    Animation<double> animation,
    Animation<double> secondaryAnimation,
    Widget child,
  ) {
    return SharedAxisTransition(
      animation: animation,
      secondaryAnimation: secondaryAnimation,
      transitionType: SharedAxisTransitionType.horizontal,
      child: child,
    );
  }
}

/// Thème global de l'application Kola.
///
/// Philosophie design (cf. DESIGN.md) : "fintech-meets-magazine".
/// - Pas d'ombres : la profondeur vient des "tonal layers" et hairline borders.
/// - Boutons et chips : toujours en pilule (rounded-full).
/// - Cards & inputs : géométrie adoucie (rounded-lg / rounded-md).
class AppTheme {
  AppTheme._();

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,

      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.primaryContainer,
        primary: AppColors.primary,
        onPrimary: AppColors.onPrimary,
        secondary: AppColors.secondary,
        onSecondary: AppColors.onSecondary,
        error: AppColors.error,
        onError: AppColors.onError,
        surface: AppColors.surface,
        onSurface: AppColors.onSurface,
      ),

      scaffoldBackgroundColor: AppColors.background,

      // --- AppBar ---
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.surface,
        foregroundColor: AppColors.onSurface,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: true,
        titleTextStyle: AppTypography.headingSm,
        iconTheme: const IconThemeData(color: AppColors.onSurface),
      ),

      // --- Texte global ---
      textTheme: TextTheme(
        displayLarge: AppTypography.displayLg,
        displayMedium: AppTypography.displayLgMobile,
        headlineMedium: AppTypography.headingMd,
        headlineSmall: AppTypography.headingSm,
        bodyLarge: AppTypography.bodyLg,
        bodyMedium: AppTypography.bodyMd,
        bodySmall: AppTypography.bodySm,
        labelSmall: AppTypography.labelXs,
        titleMedium: AppTypography.bodyMdBold,
        labelLarge: AppTypography.buttonMd,
      ),

      // --- Boutons primaires : pilule, fond cobalt, sans ombre ---
      elevatedButtonTheme: ElevatedButtonThemeData(
        style:
            ElevatedButton.styleFrom(
              backgroundColor: AppColors.primaryContainer,
              foregroundColor: AppColors.onPrimary,
              minimumSize: const Size.fromHeight(AppDimens.buttonMinHeight),
              padding: const EdgeInsets.symmetric(
                vertical: 14,
                horizontal: AppSpacing.lg,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppRadius.full),
              ),
              textStyle: AppTypography.buttonMd,
              elevation: 0,
            ).copyWith(
              overlayColor: WidgetStateProperty.all(
                Colors.white.withValues(alpha: 0.08),
              ),
            ),
      ),

      // --- Boutons secondaires : pilule, fond gris doux, texte noir ---
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          backgroundColor: AppColors.surfaceContainerHigh,
          foregroundColor: AppColors.onSurface,
          minimumSize: const Size.fromHeight(AppDimens.buttonMinHeight),
          side: BorderSide.none,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppRadius.full),
          ),
          textStyle: AppTypography.buttonMd.copyWith(
            color: AppColors.onSurface,
          ),
        ),
      ),

      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.primary,
          textStyle: AppTypography.bodySm.copyWith(
            color: AppColors.primary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),

      // --- Inputs : 56px hauteur, rounded-md, label externe, focus cobalt ---
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.surfaceCard,
        constraints: const BoxConstraints(minHeight: AppDimens.inputHeight),
        contentPadding: const EdgeInsets.symmetric(
          vertical: 16,
          horizontal: AppSpacing.md,
        ),
        hintStyle: AppTypography.bodyMd,
        labelStyle: AppTypography.bodySm.copyWith(fontWeight: FontWeight.w600),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.md),
          borderSide: const BorderSide(color: AppColors.hairlineLight),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.md),
          borderSide: const BorderSide(color: AppColors.hairlineLight),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.md),
          borderSide: const BorderSide(color: AppColors.primary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.md),
          borderSide: const BorderSide(color: AppColors.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppRadius.md),
          borderSide: const BorderSide(color: AppColors.error, width: 1.5),
        ),
      ),

      // --- Bottom nav : 4 onglets, fond blanc, actif = primary ---
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: AppColors.surfaceContainerLowest,
        selectedItemColor: AppColors.primary,
        unselectedItemColor: AppColors.onSurfaceVariant,
        selectedLabelStyle: AppTypography.labelXs.copyWith(
          color: AppColors.primary,
        ),
        unselectedLabelStyle: AppTypography.labelXs,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),

      // --- Cards : fond blanc, hairline border, pas d'ombre ---
      cardTheme: CardThemeData(
        color: AppColors.surfaceCard,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppRadius.lg),
          side: const BorderSide(color: AppColors.hairlineLight),
        ),
      ),

      dividerTheme: const DividerThemeData(
        color: AppColors.hairlineLight,
        thickness: 1,
        space: 1,
      ),

      iconTheme: const IconThemeData(color: AppColors.onSurface),

      // --- Transitions de page : shared-axis Material, cohérent partout ---
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: _SharedAxisPageTransitionsBuilder(),
          TargetPlatform.iOS: _SharedAxisPageTransitionsBuilder(),
          TargetPlatform.windows: _SharedAxisPageTransitionsBuilder(),
          TargetPlatform.macOS: _SharedAxisPageTransitionsBuilder(),
          TargetPlatform.linux: _SharedAxisPageTransitionsBuilder(),
        },
      ),
    );
  }
}
