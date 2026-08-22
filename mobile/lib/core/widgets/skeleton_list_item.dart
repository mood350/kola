import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';
import 'skeleton_box.dart';

/// Skeleton d'une ligne de liste générique (avatar rond + 2 lignes de texte +
/// bloc de fin) — reproduit la forme de [TransactionItem]/lignes KolaCard.
/// Réutilisé pour Vaults, prêts, bénéficiaires, historique, notifications.
class SkeletonListItem extends StatelessWidget {
  const SkeletonListItem({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceCard,
        borderRadius: BorderRadius.circular(AppRadius.lg),
        border: Border.all(color: AppColors.secondaryContainer),
      ),
      child: Row(
        children: [
          const SkeletonBox(
            width: AppDimens.iconBadgeSize,
            height: AppDimens.iconBadgeSize,
            borderRadius: BorderRadius.all(Radius.circular(999)),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SkeletonBox(height: 14, width: 140),
                const SizedBox(height: AppSpacing.xxs),
                SkeletonBox(height: 12, width: 90),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          const SkeletonBox(height: 14, width: 50),
        ],
      ),
    );
  }
}

/// Liste verticale de [count] skeletons espacés, pour remplir un état de
/// chargement de liste initiale.
class SkeletonList extends StatelessWidget {
  final int count;

  const SkeletonList({super.key, this.count = 4});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List.generate(
        count,
        (index) => Padding(
          padding: const EdgeInsets.only(bottom: AppSpacing.sm),
          child: const SkeletonListItem(),
        ),
      ),
    );
  }
}
