import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../core/theme/kola_icons.dart';
import 'package:provider/provider.dart';

import '../core/theme/app_colors.dart';
import '../core/theme/app_spacing.dart';
import '../core/theme/app_typography.dart';
import '../core/utils/formatters.dart';
import '../core/utils/transaction_display.dart';
import '../core/widgets/kola_shell.dart';
import '../core/widgets/kola_ui.dart';
import '../core/widgets/transaction_tile.dart';
import '../models/transaction.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';

enum _Filter { all, incoming, outgoing }

class TransactionHistoryScreen extends StatefulWidget {
  const TransactionHistoryScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<TransactionHistoryScreen> createState() =>
      _TransactionHistoryScreenState();
}

class _TransactionHistoryScreenState extends State<TransactionHistoryScreen> {
  _Filter _filter = _Filter.all;

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final all = data.transactions;

    final visible = all.where((tx) {
      return switch (_filter) {
        _Filter.all => true,
        _Filter.incoming => isIncoming(tx),
        _Filter.outgoing => !isIncoming(tx),
      };
    }).toList()..sort((a, b) => b.displayedAt.compareTo(a.displayedAt));

    // Résumé du mois en cours : seules les opérations réussies comptent, une
    // transaction échouée n'a déplacé aucun argent.
    final now = DateTime.now();
    var incoming = 0.0;
    var outgoing = 0.0;
    for (final tx in all) {
      final at = tx.displayedAt;
      if (!tx.isCompleted || at.year != now.year || at.month != now.month) {
        continue;
      }
      final signed = signedAmount(tx);
      if (signed > 0) {
        incoming += signed;
      } else {
        outgoing += -signed;
      }
    }

    final byDay = <DateTime, List<KolaTransaction>>{};
    for (final tx in visible) {
      final at = tx.displayedAt;
      byDay.putIfAbsent(DateTime(at.year, at.month, at.day), () => []).add(tx);
    }

    return KolaScreen(
      route: KolaRoute.transactionHistory,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Retour',
          onTap: () => widget.onNavigate(KolaRoute.home),
        ),
        Text('Transactions', style: AppTypography.screenTitle),
        const SizedBox(height: AppSpacing.md),
        _MonthSummary(incoming: incoming, outgoing: outgoing),
        const SizedBox(height: AppSpacing.md),
        _FilterBar(
          selected: _filter,
          onChanged: (filter) => setState(() => _filter = filter),
        ),
        if (!data.loading && byDay.isEmpty)
          const Padding(
            padding: EdgeInsets.only(top: 18),
            child: KolaCard(
              padding: EdgeInsets.symmetric(vertical: 28),
              child: EmptyState(
                icon: KolaIcons.receiptOutline,
                title: 'Aucune transaction à afficher.',
              ),
            ),
          ),
        for (final entry in byDay.entries) ...[
          Padding(
            padding: const EdgeInsets.only(top: 22, bottom: 8, left: 4),
            child: Text(
              _dayLabel(entry.key),
              style: AppTypography.smallBold.copyWith(color: AppColors.muted),
            ),
          ),
          KolaCard(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
            child: Column(
              children: [
                for (var i = 0; i < entry.value.length; i++)
                  TransactionTile(
                    transaction: entry.value[i],
                    withDivider: i > 0,
                    onTap: () => _showDetail(entry.value[i]),
                  ),
              ],
            ),
          ),
        ],
      ],
    );
  }

  void _showDetail(KolaTransaction tx) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _DetailSheet(transaction: tx),
    );
  }
}

/// « Aujourd'hui », « Hier », puis « Mardi 24 septembre ».
String _dayLabel(DateTime day) {
  final now = DateTime.now();
  final today = DateTime(now.year, now.month, now.day);
  if (day == today) return 'Aujourd’hui';
  if (day == today.subtract(const Duration(days: 1))) return 'Hier';
  final text = DateFormat('EEEE d MMMM', 'fr_FR').format(day);
  return text[0].toUpperCase() + text.substring(1);
}

class _MonthSummary extends StatelessWidget {
  const _MonthSummary({required this.incoming, required this.outgoing});

  final double incoming;
  final double outgoing;

  @override
  Widget build(BuildContext context) {
    final month = DateFormat('MMMM', 'fr_FR').format(DateTime.now());
    return KolaCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Ce mois-ci · $month',
            style: AppTypography.small.copyWith(color: AppColors.muted),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: _Figure(
                  label: 'Entrées',
                  value: '+${money(incoming)} F',
                  color: AppColors.greenDark,
                ),
              ),
              Container(width: 1, height: 36, color: AppColors.hairline),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: _Figure(
                  label: 'Sorties',
                  value: '−${money(outgoing)} F',
                  color: AppColors.ink,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Figure extends StatelessWidget {
  const _Figure({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: AppTypography.small),
        const SizedBox(height: 2),
        Text(
          value,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: AppTypography.amount.copyWith(fontSize: 18, color: color),
        ),
      ],
    );
  }
}

class _FilterBar extends StatelessWidget {
  const _FilterBar({required this.selected, required this.onChanged});

  final _Filter selected;
  final ValueChanged<_Filter> onChanged;

  static const _labels = {
    _Filter.all: 'Toutes',
    _Filter.incoming: 'Entrées',
    _Filter.outgoing: 'Sorties',
  };

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        for (final entry in _labels.entries)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: GestureDetector(
              onTap: () => onChanged(entry.key),
              behavior: HitTestBehavior.opaque,
              child: Container(
                height: 36,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: selected == entry.key
                      ? AppColors.primary
                      : AppColors.pale2,
                  borderRadius: BorderRadius.circular(18),
                ),
                child: Text(
                  entry.value,
                  style: AppTypography.smallBold.copyWith(
                    color: selected == entry.key
                        ? AppColors.white
                        : AppColors.primary,
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _DetailSheet extends StatelessWidget {
  const _DetailSheet({required this.transaction});

  final KolaTransaction transaction;

  @override
  Widget build(BuildContext context) {
    final meta = metaFor(transaction.type);
    final incoming = isIncoming(transaction);
    final failed = transaction.isFailed;
    final note = transactionNote(transaction);
    final title = transactionTitle(transaction);
    final amount = signedAmount(transaction);

    return Container(
      padding: const EdgeInsets.fromLTRB(22, 22, 22, 30),
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppRadius.sheet),
        ),
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 38,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 22),
            Center(
              child: IconCircle(
                meta.icon,
                size: 26,
                diameter: 60,
                background: failed
                    ? AppColors.redPale
                    : incoming
                    ? AppColors.greenPale
                    : AppColors.pale2,
                color: failed
                    ? AppColors.red
                    : incoming
                    ? AppColors.greenDark
                    : AppColors.primary,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              title,
              textAlign: TextAlign.center,
              style: AppTypography.cardTitle,
            ),
            if (title != meta.label) ...[
              const SizedBox(height: 2),
              Text(
                meta.label,
                textAlign: TextAlign.center,
                style: AppTypography.small,
              ),
            ],
            const SizedBox(height: 10),
            Text(
              '${incoming ? '+' : '−'}${money(amount.abs())} FCFA',
              textAlign: TextAlign.center,
              style: AppTypography.screenTitle.copyWith(
                fontSize: 28,
                color: failed
                    ? AppColors.muted
                    : incoming
                    ? AppColors.greenDark
                    : AppColors.ink,
                decoration: failed ? TextDecoration.lineThrough : null,
              ),
            ),
            const SizedBox(height: 10),
            Center(
              child: Pill(
                statusLabel(transaction),
                green: transaction.isCompleted,
                red: failed,
              ),
            ),
            Container(
              margin: const EdgeInsets.only(top: 20, bottom: 14),
              padding: const EdgeInsets.symmetric(horizontal: 14),
              decoration: BoxDecoration(
                color: AppColors.pale,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Column(
                children: [
                  DetailRow(
                    label: 'Date',
                    value: receiptDate(transaction.displayedAt),
                    last:
                        note == null &&
                        transaction.fee <= 0 &&
                        transaction.failureReason == null,
                  ),
                  if (note != null && note != title)
                    DetailRow(
                      label: 'Motif',
                      value: note,
                      last:
                          transaction.fee <= 0 &&
                          transaction.failureReason == null,
                    ),
                  if (transaction.fee > 0)
                    DetailRow(
                      label: 'Frais',
                      value: '${money(transaction.fee)} FCFA',
                      last: transaction.failureReason == null,
                    ),
                  if (failed && transaction.failureReason != null)
                    DetailRow(
                      label: 'Cause',
                      value: transaction.failureReason!,
                      last: true,
                    ),
                ],
              ),
            ),
            Center(
              child: SelectableText(
                'Réf. ${transaction.reference}',
                style: AppTypography.small,
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            PrimaryButton(
              label: 'Fermer',
              onPressed: () => Navigator.of(context).pop(),
            ),
          ],
        ),
      ),
    );
  }
}
