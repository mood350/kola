import 'package:flutter/material.dart';
import '../theme/kola_icons.dart';

import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';
import '../theme/app_typography.dart';

/// Carte blanche à filet clair et ombre douce : le conteneur de base de tous
/// les écrans.
class KolaCard extends StatelessWidget {
  const KolaCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(AppSpacing.md),
    this.color = AppColors.white,
    this.width,
    this.onTap,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color color;
  final double? width;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final card = Container(
      width: width,
      padding: padding,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(AppRadius.card),
        border: Border.all(color: AppColors.hairline),
        boxShadow: AppColors.softShadow,
      ),
      child: child,
    );
    if (onTap == null) return card;
    return GestureDetector(onTap: onTap, child: card);
  }
}

/// Pastille circulaire colorée portant une icône — utilisée devant chaque
/// ligne de transaction, action rapide ou coffre.
class IconCircle extends StatelessWidget {
  const IconCircle(
    this.icon, {
    super.key,
    this.color = AppColors.primary,
    this.background = AppColors.pale2,
    this.size = 22,
    this.diameter = AppDimens.iconCircle,
  });

  final IconData icon;
  final Color color;
  final Color background;
  final double size;
  final double diameter;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: diameter,
      height: diameter,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: background, shape: BoxShape.circle),
      child: Icon(icon, size: size, color: color),
    );
  }
}

/// Étiquette compacte (statut, tier KYC, mention "0 frais").
class Pill extends StatelessWidget {
  const Pill(this.label, {super.key, this.green = false, this.red = false});

  final String label;
  final bool green;
  final bool red;

  @override
  Widget build(BuildContext context) {
    final background = red
        ? AppColors.redPale
        : green
        ? AppColors.greenPale
        : AppColors.pale2;
    final foreground = red
        ? AppColors.redDark
        : green
        ? AppColors.greenDark
        : AppColors.primary;
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xs,
        vertical: 4,
      ),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: AppTypography.badge.copyWith(color: foreground),
      ),
    );
  }
}

/// Jauge de progression d'un coffre ou d'un remboursement.
class KolaProgress extends StatelessWidget {
  const KolaProgress(this.percent, {super.key, this.color = AppColors.green});

  final double percent;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(4),
      child: LinearProgressIndicator(
        value: (percent / 100).clamp(0.0, 1.0),
        minHeight: 7,
        backgroundColor: const Color(0xFFE0E5FA),
        valueColor: AlwaysStoppedAnimation<Color>(color),
      ),
    );
  }
}

/// Bouton d'action principal : jaune par défaut, marine en variante `blue`.
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({
    super.key,
    required this.label,
    this.onPressed,
    this.blue = false,
    this.loading = false,
    this.icon,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool blue;
  final bool loading;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final enabled = onPressed != null && !loading;
    final background = blue ? AppColors.primary : AppColors.yellow;
    final foreground = blue ? AppColors.white : AppColors.primary;
    return Opacity(
      opacity: enabled ? 1 : 0.55,
      child: Material(
        color: background,
        borderRadius: BorderRadius.circular(25),
        child: InkWell(
          onTap: enabled ? onPressed : null,
          borderRadius: BorderRadius.circular(25),
          child: SizedBox(
            height: AppDimens.buttonHeight,
            child: Center(
              child: loading
                  ? SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.4,
                        valueColor: AlwaysStoppedAnimation<Color>(foreground),
                      ),
                    )
                  : Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        if (icon != null) ...[
                          Icon(icon, size: 18, color: foreground),
                          const SizedBox(width: AppSpacing.xs),
                        ],
                        Flexible(
                          child: Text(
                            label,
                            textAlign: TextAlign.center,
                            style: AppTypography.button.copyWith(
                              color: foreground,
                            ),
                          ),
                        ),
                      ],
                    ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Titre de section, avec une action facultative alignée à droite.
class SectionTitle extends StatelessWidget {
  const SectionTitle(this.title, {super.key, this.action, this.onAction});

  final String title;
  final String? action;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.lg, bottom: 13),
      child: Row(
        children: [
          Expanded(child: Text(title, style: AppTypography.section)),
          if (action != null)
            GestureDetector(
              onTap: onAction,
              child: Text(
                action!,
                style: AppTypography.badge.copyWith(
                  fontSize: 10,
                  color: AppColors.primary,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// Intitulé de champ de formulaire, en capitales au-dessus de la saisie.
class FieldLabel extends StatelessWidget {
  const FieldLabel(this.label, {super.key});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 18, bottom: 7),
      child: Text(label.toUpperCase(), style: AppTypography.fieldLabel),
    );
  }
}

/// Bandeau d'état vide, affiché à la place d'une liste sans contenu.
class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.message,
  });

  final IconData icon;
  final String title;
  final String? message;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 130),
      alignment: Alignment.center,
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 30, color: AppColors.muted),
          const SizedBox(height: AppSpacing.xs),
          Text(
            title,
            textAlign: TextAlign.center,
            style: AppTypography.small.copyWith(
              fontWeight: FontWeight.w800,
              color: AppColors.ink,
            ),
          ),
          if (message != null) ...[
            const SizedBox(height: 3),
            Text(
              message!,
              textAlign: TextAlign.center,
              style: AppTypography.caption.copyWith(fontSize: 9),
            ),
          ],
        ],
      ),
    );
  }
}

/// Lien de retour en haut d'un écran de détail.
class BackLink extends StatelessWidget {
  const BackLink({super.key, required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: SizedBox(
          height: 38,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                KolaIcons.arrowBack,
                size: 19,
                color: AppColors.primary,
              ),
              const SizedBox(width: 7),
              Text(label, style: AppTypography.smallBold),
            ],
          ),
        ),
      ),
    );
  }
}

/// Ligne « intitulé / valeur » d'un récapitulatif ou d'un reçu.
class DetailRow extends StatelessWidget {
  const DetailRow({
    super.key,
    required this.label,
    required this.value,
    this.last = false,
  });

  final String label;
  final String value;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minHeight: 45),
      decoration: last
          ? null
          : const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppColors.border)),
            ),
      child: Row(
        children: [
          Expanded(child: Text(label, style: AppTypography.caption)),
          const SizedBox(width: AppSpacing.sm),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: AppTypography.caption.copyWith(
                fontWeight: FontWeight.w800,
                color: AppColors.ink,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// En-tête de page : sur-titre vert, titre, sous-titre, et pictogramme marine.
class PageHeader extends StatelessWidget {
  const PageHeader({
    super.key,
    required this.overline,
    required this.title,
    required this.icon,
    this.subtitle,
  });

  final String overline;
  final String title;
  final IconData icon;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                overline,
                style: AppTypography.badge.copyWith(
                  fontSize: 8,
                  fontWeight: FontWeight.w900,
                  color: AppColors.greenDark,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                title,
                style: AppTypography.screenTitle.copyWith(fontSize: 24),
              ),
              if (subtitle != null) ...[
                const SizedBox(height: 3),
                Text(subtitle!, style: AppTypography.caption),
              ],
            ],
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Container(
          width: 52,
          height: 52,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(17),
          ),
          child: Icon(icon, size: 25, color: AppColors.yellow),
        ),
      ],
    );
  }
}

/// Ligne d'en-tête d'une feuille modale : titre, sous-titre et croix.
class SheetHeader extends StatelessWidget {
  const SheetHeader({super.key, required this.title, this.subtitle});

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: AppTypography.screenTitle.copyWith(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
              if (subtitle != null) ...[
                const SizedBox(height: 4),
                Text(subtitle!, style: AppTypography.small),
              ],
            ],
          ),
        ),
        IconButton(
          onPressed: () => Navigator.of(context).maybePop(),
          icon: const Icon(KolaIcons.close, size: 24, color: AppColors.ink),
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(),
        ),
      ],
    );
  }
}
