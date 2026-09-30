import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../models/transaction.dart';
import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';
import '../theme/app_typography.dart';
import '../utils/date_format_utils.dart';
import '../utils/formatters.dart';
import '../utils/transaction_display.dart';

/// Ligne de transaction, sur le modèle des applications de paiement : un
/// avatar, un titre (qui, ou quoi), une ligne discrète (type et heure), le
/// montant signé à droite. Ni référence ni statut quand tout s'est bien
/// passé : seul ce qui sort de l'ordinaire (attente, échec) est signalé.
class TransactionTile extends StatelessWidget {
  const TransactionTile({
    super.key,
    required this.transaction,
    this.withDivider = false,
    this.showDay = false,
    this.onTap,
  });

  final KolaTransaction transaction;
  final bool withDivider;

  /// Inclut le jour avant l'heure. Inutile sous un en-tête de date, utile
  /// dans une liste qui mélange les jours (accueil).
  final bool showDay;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final meta = metaFor(transaction.type);
    final incoming = isIncoming(transaction);
    final failed = transaction.isFailed;
    final title = transactionTitle(transaction);

    final details = <String>[
      if (title != meta.label) meta.label,
      showDay
          ? formatRelativeDate(transaction.displayedAt)
          : DateFormat('HH:mm').format(transaction.displayedAt),
    ];

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        constraints: const BoxConstraints(minHeight: 68),
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: withDivider
            ? const BoxDecoration(
                border: Border(top: BorderSide(color: AppColors.hairline)),
              )
            : null,
        child: Row(
          children: [
            _Avatar(transaction: transaction),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.bodyBold.copyWith(
                      color: failed ? AppColors.muted : AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    details.join(' · '),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.small,
                  ),
                  if (!transaction.isCompleted) ...[
                    const SizedBox(height: 3),
                    _StatusDot(transaction: transaction),
                  ],
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.xs),
            Text(
              '${incoming ? '+' : '−'}${money(signedAmount(transaction).abs())} F',
              textAlign: TextAlign.right,
              style: AppTypography.bodyBold.copyWith(
                color: failed
                    ? AppColors.muted
                    : incoming
                    ? AppColors.greenDark
                    : AppColors.ink,
                decoration: failed ? TextDecoration.lineThrough : null,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({required this.transaction});

  final KolaTransaction transaction;

  @override
  Widget build(BuildContext context) {
    final meta = metaFor(transaction.type);
    final failed = transaction.isFailed;
    final incoming = isIncoming(transaction);
    final initials = transactionInitials(transaction);

    final background = failed
        ? AppColors.redPale
        : incoming
        ? AppColors.greenPale
        : AppColors.pale2;
    final foreground = failed
        ? AppColors.red
        : incoming
        ? AppColors.greenDark
        : AppColors.primary;

    return Container(
      width: 44,
      height: 44,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: background, shape: BoxShape.circle),
      child: initials != null
          ? Text(
              initials,
              style: AppTypography.bodyBold.copyWith(color: foreground),
            )
          : Icon(meta.icon, size: 20, color: foreground),
    );
  }
}

class _StatusDot extends StatelessWidget {
  const _StatusDot({required this.transaction});

  final KolaTransaction transaction;

  @override
  Widget build(BuildContext context) {
    final failed = transaction.isFailed;
    final color = failed ? AppColors.red : AppColors.yellowDark;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 6,
          height: 6,
          decoration: BoxDecoration(color: color, shape: BoxShape.circle),
        ),
        const SizedBox(width: 5),
        Text(
          statusLabel(transaction),
          style: AppTypography.small.copyWith(
            color: color,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}
