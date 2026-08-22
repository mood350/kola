import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/utils/transaction_display_utils.dart';
import '../../core/widgets/kola_card.dart';
import '../../core/widgets/status_pill.dart';
import '../../models/transaction.dart';
import '../../providers/transaction_provider.dart';

/// Détail complet d'une transaction (GET /api/transactions/{reference}).
class TransactionDetailScreen extends StatefulWidget {
  final String reference;

  const TransactionDetailScreen({super.key, required this.reference});

  @override
  State<TransactionDetailScreen> createState() =>
      _TransactionDetailScreenState();
}

class _TransactionDetailScreenState extends State<TransactionDetailScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<TransactionProvider>().loadDetail(widget.reference);
    });
  }

  StatusPillTone _toneFor(TransactionStatus status) {
    switch (status) {
      case TransactionStatus.success:
        return StatusPillTone.success;
      case TransactionStatus.pending:
        return StatusPillTone.warning;
      case TransactionStatus.failed:
        return StatusPillTone.danger;
      case TransactionStatus.cancelled:
        return StatusPillTone.neutral;
      case TransactionStatus.refunded:
        return StatusPillTone.info;
    }
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TransactionProvider>();
    final tx = provider.detail;
    final currencyFormat = NumberFormat.decimalPattern('fr_FR');

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        elevation: 0,
        title: Text('Détail', style: AppTypography.headingSm),
      ),
      body: SafeArea(
        child: Builder(
          builder: (context) {
            if (provider.isLoadingDetail && tx == null) {
              return const Center(
                child: CircularProgressIndicator(color: AppColors.primary),
              );
            }
            if (provider.detailError != null && tx == null) {
              return Center(
                child: Text(
                  provider.detailError!,
                  style: AppTypography.bodyMd,
                  textAlign: TextAlign.center,
                ),
              );
            }
            if (tx == null) return const SizedBox.shrink();

            return SingleChildScrollView(
              padding: const EdgeInsets.all(AppSpacing.marginMobile),
              child: KolaCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: AppDimens.iconBadgeSize,
                          height: AppDimens.iconBadgeSize,
                          decoration: BoxDecoration(
                            color: colorForTransactionType(
                              tx.type,
                            ).withValues(alpha: 0.1),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            tx.icon,
                            color: colorForTransactionType(tx.type),
                          ),
                        ),
                        const SizedBox(width: AppSpacing.md),
                        Expanded(
                          child: Text(
                            tx.displayTitle,
                            style: AppTypography.headingSm,
                          ),
                        ),
                        StatusPill(
                          label: tx.status.label,
                          tone: _toneFor(tx.status),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    Text(
                      '${tx.isCredit ? '+' : '-'}${currencyFormat.format(tx.amount)} ${tx.currency}',
                      style: AppTypography.displayLgMobile.copyWith(
                        color: tx.isCredit
                            ? AppColors.success
                            : AppColors.onSurface,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    _InfoRow(label: 'Référence', value: tx.reference),
                    if (tx.fee > 0)
                      _InfoRow(
                        label: 'Frais',
                        value:
                            '${currencyFormat.format(tx.fee)} ${tx.currency}',
                      ),
                    if (tx.receiverPhoneNumber != null)
                      _InfoRow(
                        label: 'Destinataire',
                        value: tx.receiverPhoneNumber!,
                      ),
                    if (tx.description != null)
                      _InfoRow(label: 'Description', value: tx.description!),
                    _InfoRow(
                      label: 'Date',
                      value:
                          '${tx.createdAt.day.toString().padLeft(2, '0')}/${tx.createdAt.month.toString().padLeft(2, '0')}/${tx.createdAt.year} '
                          '${tx.createdAt.hour.toString().padLeft(2, '0')}:${tx.createdAt.minute.toString().padLeft(2, '0')}',
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: AppSpacing.xs),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: AppTypography.bodySm),
          Flexible(
            child: Text(
              value,
              style: AppTypography.bodyMdBold,
              textAlign: TextAlign.end,
            ),
          ),
        ],
      ),
    );
  }
}
