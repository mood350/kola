import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/widgets/skeleton_box.dart';
import '../../../core/widgets/skeleton_list_item.dart';

/// Skeleton de l'onglet Home : carte de solde + 3 lignes de transaction,
/// affiché pendant le tout premier chargement (avant que le wallet existe).
class HomeSkeleton extends StatelessWidget {
  const HomeSkeleton({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(AppDimens.cardPadding),
          decoration: BoxDecoration(
            color: AppColors.surfaceCard,
            borderRadius: BorderRadius.circular(AppRadius.xl),
            border: Border.all(color: AppColors.secondaryContainer),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SkeletonBox(
                height: 24,
                width: 90,
                borderRadius: BorderRadius.all(Radius.circular(999)),
              ),
              const SizedBox(height: AppSpacing.md),
              const SkeletonBox(height: 14, width: 100),
              const SizedBox(height: AppSpacing.xs),
              const SkeletonBox(height: 36, width: 180),
              const SizedBox(height: AppSpacing.lg),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: List.generate(
                  4,
                  (_) => const SkeletonBox(
                    width: AppDimens.iconActionSize,
                    height: AppDimens.iconActionSize,
                    borderRadius: BorderRadius.all(Radius.circular(999)),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.xl),
        const SkeletonBox(height: 20, width: 100),
        const SizedBox(height: AppSpacing.md),
        const SkeletonList(count: 3),
      ],
    );
  }
}
