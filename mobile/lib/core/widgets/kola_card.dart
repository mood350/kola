import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';

/// Card de base Kola : fond blanc, hairline border, sans ombre.
/// Padding interne par défaut = xl (32px), cf. DESIGN.md "Cards & Inputs".
///
/// Implementee avec [Material] et non un Container decore, pour une raison de
/// rendu et pas de style : l'encre (ripples des InkWell, fonds et splashes des
/// ListTile) est toujours peinte sur le Material ANCESTOR le plus proche. Avec
/// un simple Container, ce Material est celui du Scaffold, situe DERRIERE le
/// fond blanc opaque de la carte — l'encre existait donc mais restait
/// invisible, pour le onTap de la carte comme pour tout ListTile place dedans
/// (d'ou l'assertion « ListTile background color or ink splashes may be
/// invisible » declenchee par l'ExpansionTile de l'ecran Credit).
class KolaCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final Color? backgroundColor;
  final VoidCallback? onTap;

  const KolaCard({
    super.key,
    required this.child,
    this.padding,
    this.backgroundColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final content = Padding(
      padding: padding ?? const EdgeInsets.all(AppDimens.cardPadding),
      child: child,
    );

    return Material(
      // elevation 0 par defaut : le rendu reste « sans ombre ».
      color: backgroundColor ?? AppColors.surfaceCard,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.lg),
        side: const BorderSide(color: AppColors.hairlineLight),
      ),
      // Sans clip, un ripple deborderait des coins arrondis.
      clipBehavior: Clip.antiAlias,
      child: onTap == null ? content : InkWell(onTap: onTap, child: content),
    );
  }
}
