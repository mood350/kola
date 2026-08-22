import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';
import '../theme/app_spacing.dart';

/// Tonalité sémantique d'un [StatusPill].
enum StatusPillTone { success, warning, danger, neutral, info }

/// Pastille de statut générique (coffre, prêt, transaction) — distincte de
/// [KycTierBadge] qui reste dédié au niveau KYC. Réutilisée partout où un
/// statut backend (VaultStatus/LoanStatus/TransactionStatus) doit s'afficher.
class StatusPill extends StatelessWidget {
  final String label;
  final StatusPillTone tone;

  const StatusPill({super.key, required this.label, required this.tone});

  Color get _color {
    switch (tone) {
      case StatusPillTone.success:
        return AppColors.success;
      case StatusPillTone.warning:
        return AppColors.warning;
      case StatusPillTone.danger:
        return AppColors.danger;
      case StatusPillTone.neutral:
        return AppColors.outline;
      case StatusPillTone.info:
        return AppColors.primary;
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _color;
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppRadius.full),
      ),
      child: Text(label, style: AppTypography.labelXs.copyWith(color: color)),
    );
  }
}
