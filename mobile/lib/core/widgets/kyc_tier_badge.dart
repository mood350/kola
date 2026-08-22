import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';
import '../theme/app_spacing.dart';

enum KycBadgeStatus { verified, pending, locked }

/// Badge de niveau KYC (TIER_0 à TIER_3).
/// Vert pour "Vérifié", orange pour "En attente" (cf. DESIGN.md - Fintech Specifics).
class KycTierBadge extends StatelessWidget {
  final String label; // ex: "NIVEAU 1"
  final KycBadgeStatus status;

  const KycTierBadge({
    super.key,
    required this.label,
    this.status = KycBadgeStatus.verified,
  });

  Color get _dotColor {
    switch (status) {
      case KycBadgeStatus.verified:
        return AppColors.success;
      case KycBadgeStatus.pending:
        return AppColors.warning;
      case KycBadgeStatus.locked:
        return AppColors.outline;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(AppRadius.full),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(color: _dotColor, shape: BoxShape.circle),
          ),
          const SizedBox(width: AppSpacing.xs),
          Text(
            label.toUpperCase(),
            style: AppTypography.labelXs.copyWith(color: AppColors.onSurface),
          ),
        ],
      ),
    );
  }
}
