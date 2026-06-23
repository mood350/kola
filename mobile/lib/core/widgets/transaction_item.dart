import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';
import '../theme/app_spacing.dart';

/// Item de transaction réutilisable (liste "Récent", Historique complet, Détail).
/// Card blanche, hairline border, icône colorée selon le type de transaction.
class TransactionItem extends StatelessWidget {
  final IconData icon;
  final Color iconColor;
  final String title;
  final String subtitle;
  final String amount;
  final String dateLabel;
  final bool isPositive;
  final VoidCallback? onTap;

  const TransactionItem({
    super.key,
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.subtitle,
    required this.amount,
    required this.dateLabel,
    this.isPositive = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppRadius.lg),
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.md),
        decoration: BoxDecoration(
          color: AppColors.surfaceCard,
          borderRadius: BorderRadius.circular(AppRadius.lg),
          border: Border.all(color: AppColors.secondaryContainer),
        ),
        child: Row(
          children: [
            Container(
              width: AppDimens.iconBadgeSize,
              height: AppDimens.iconBadgeSize,
              decoration: BoxDecoration(
                color: iconColor.withValues(alpha: 0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: iconColor, size: 22),
            ),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: AppTypography.bodyMdBold),
                  Text(subtitle, style: AppTypography.bodySm),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  amount,
                  style: AppTypography.bodyMdBold.copyWith(
                    color: isPositive ? AppColors.success : AppColors.onSurface,
                  ),
                ),
                Text(dateLabel, style: AppTypography.labelXs),
              ],
            ),
          ],
        ),
      ),
    );
  }
}