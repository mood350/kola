import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';
import '../theme/app_colors.dart';

/// Placeholder rectangulaire animé (shimmer) — brique de base des états de
/// chargement "en forme du contenu réel", à préférer à un simple spinner
/// pour tout chargement initial de contenu (liste, carte, profil).
/// Les spinners restent corrects pour le feedback d'une action ponctuelle
/// (ex: PrimaryButton.isLoading) — cf. mobile/CLAUDE.md skeleton guidance.
class SkeletonBox extends StatelessWidget {
  final double width;
  final double height;
  final BorderRadiusGeometry borderRadius;

  const SkeletonBox({
    super.key,
    this.width = double.infinity,
    required this.height,
    this.borderRadius = const BorderRadius.all(Radius.circular(8)),
  });

  @override
  Widget build(BuildContext context) {
    return Shimmer.fromColors(
      baseColor: AppColors.surfaceContainerLow,
      highlightColor: AppColors.surfaceContainerHighest,
      child: Container(
        width: width,
        height: height,
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLow,
          borderRadius: borderRadius,
        ),
      ),
    );
  }
}
