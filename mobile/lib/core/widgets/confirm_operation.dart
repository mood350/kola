import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';
import '../theme/app_typography.dart';
import 'kola_ui.dart';

/// Récapitulatif à valider avant tout mouvement d'argent.
///
/// Résout `true` uniquement sur « Confirmer » : un tap hors de la fenêtre ou
/// le bouton retour valent une annulation, jamais une validation par défaut.
Future<bool> confirmOperation(
  BuildContext context, {
  required String title,
  required String amount,
  required List<(String, String)> details,
  String confirmLabel = 'Confirmer',
  String? note,
}) async {
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (dialogContext) => Dialog(
      backgroundColor: AppColors.white,
      insetPadding: const EdgeInsets.symmetric(horizontal: 22),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadius.sheet),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(22, 22, 22, 14),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              title,
              style: AppTypography.screenTitle.copyWith(
                fontSize: 19,
                fontWeight: FontWeight.w800,
                color: AppColors.primary,
              ),
            ),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.symmetric(vertical: 14),
              decoration: BoxDecoration(
                color: AppColors.pale,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                amount,
                textAlign: TextAlign.center,
                style: AppTypography.amount.copyWith(fontSize: 24),
              ),
            ),
            const SizedBox(height: 6),
            for (final (index, (label, value)) in details.indexed)
              DetailRow(
                label: label,
                value: value,
                last: index == details.length - 1,
              ),
            if (note != null) ...[
              const SizedBox(height: 8),
              Text(note, style: AppTypography.caption.copyWith(fontSize: 10)),
            ],
            const SizedBox(height: AppSpacing.md),
            PrimaryButton(
              label: confirmLabel,
              onPressed: () => Navigator.of(dialogContext).pop(true),
            ),
            const SizedBox(height: 4),
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: Text(
                'Annuler',
                style: AppTypography.smallBold.copyWith(color: AppColors.muted),
              ),
            ),
          ],
        ),
      ),
    ),
  );
  return confirmed ?? false;
}
