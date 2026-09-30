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
import '../models/transaction.dart';
import '../providers/kola_data_provider.dart';
import '../routes/kola_route.dart';

enum _Filter { all, incoming, outgoing }

/// Types qui créditent le compte. Ils déterminent le signe affiché et le
/// classement entrées/sorties du relevé.
const _incomingTypes = {'CASH_IN', 'REFUND', 'LOAN_DISBURSEMENT'};

class TransactionHistoryScreen extends StatefulWidget {
  const TransactionHistoryScreen({super.key, required this.onNavigate});

  final void Function(KolaRoute) onNavigate;

  @override
  State<TransactionHistoryScreen> createState() =>
      _TransactionHistoryScreenState();
}

class _TransactionHistoryScreenState extends State<TransactionHistoryScreen> {
  _Filter _filter = _Filter.all;

  bool _isIncoming(KolaTransaction tx) => _incomingTypes.contains(tx.type);

  /// Montant signé : une sortie coûte le total débité (frais compris), pas
  /// seulement le montant transféré.
  double _signedAmount(KolaTransaction tx) => _isIncoming(tx)
      ? tx.amount
      : -(tx.totalDebited > 0 ? tx.totalDebited : tx.amount);

  @override
  Widget build(BuildContext context) {
    final data = context.watch<KolaDataProvider>();
    final all = data.transactions;

    final visible = all.where((tx) {
      return switch (_filter) {
        _Filter.all => true,
        _Filter.incoming => _isIncoming(tx),
        _Filter.outgoing => !_isIncoming(tx),
      };
    }).toList()..sort((a, b) => b.displayedAt.compareTo(a.displayedAt));

    var incoming = 0.0;
    var outgoing = 0.0;
    for (final tx in all) {
      final signed = _signedAmount(tx);
      if (signed > 0) {
        incoming += signed;
      } else {
        outgoing += -signed;
      }
    }

    final monthly = <String, List<KolaTransaction>>{};
    final monthFormat = DateFormat('MMMM y', 'fr_FR');
    for (final tx in visible) {
      monthly.putIfAbsent(monthFormat.format(tx.displayedAt), () => []).add(tx);
    }

    return KolaScreen(
      route: KolaRoute.transactionHistory,
      onNavigate: widget.onNavigate,
      children: [
        BackLink(
          label: 'Mon profil',
          onTap: () => widget.onNavigate(KolaRoute.profile),
        ),
        const PageHeader(
          overline: 'RELEVÉ KOLA',
          title: 'Historique des transactions',
          subtitle: 'Toutes les opérations de votre portefeuille.',
          icon: KolaIcons.receiptOutline,
        ),
        const SizedBox(height: 17),
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              Expanded(
                child: _Summary(
                  icon: KolaIcons.arrowDown,
                  iconColor: AppColors.mint,
                  label: 'ENTRÉES',
                  value: '+${money(incoming)} F',
                  valueColor: AppColors.mint,
                ),
              ),
              Container(
                width: 1,
                height: 46,
                margin: const EdgeInsets.symmetric(horizontal: 15),
                color: AppColors.white.withValues(alpha: 0.13),
              ),
              Expanded(
                child: _Summary(
                  icon: KolaIcons.arrowUp,
                  iconColor: AppColors.yellow,
                  label: 'SORTIES',
                  value: '-${money(outgoing)} F',
                  valueColor: AppColors.yellow,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 17),
        Container(
          height: 42,
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: AppColors.pale2,
            borderRadius: BorderRadius.circular(21),
          ),
          child: Row(
            children: [
              for (final entry in const {
                _Filter.all: 'Toutes',
                _Filter.incoming: 'Entrées',
                _Filter.outgoing: 'Sorties',
              }.entries)
                Expanded(
                  child: GestureDetector(
                    onTap: () => setState(() => _filter = entry.key),
                    behavior: HitTestBehavior.opaque,
                    child: Container(
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: _filter == entry.key
                            ? AppColors.white
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(18),
                      ),
                      child: Text(
                        entry.value,
                        style: AppTypography.caption.copyWith(
                          fontWeight: _filter == entry.key
                              ? FontWeight.w800
                              : FontWeight.w400,
                          color: _filter == entry.key
                              ? AppColors.primary
                              : AppColors.muted,
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
        if (!data.loading && monthly.isEmpty)
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
        for (final entry in monthly.entries) ...[
          Padding(
            padding: const EdgeInsets.only(top: 20, bottom: 8),
            child: Text(
              entry.key.toUpperCase(),
              style: AppTypography.fieldLabel.copyWith(fontSize: 8),
            ),
          ),
          KolaCard(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
            child: Column(
              children: [
                for (var i = 0; i < entry.value.length; i++)
                  _HistoryRow(
                    transaction: entry.value[i],
                    incoming: _isIncoming(entry.value[i]),
                    amount: _signedAmount(entry.value[i]),
                    withDivider: i > 0,
                    onTap: () => _showDetail(entry.value[i]),
                  ),
              ],
            ),
          ),
        ],
        Padding(
          padding: const EdgeInsets.only(top: 14),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                KolaIcons.shieldCheckmarkOutline,
                size: 17,
                color: AppColors.greenDark,
              ),
              const SizedBox(width: 7),
              Text(
                'Historique sécurisé et disponible à tout moment.',
                style: AppTypography.badge.copyWith(
                  fontSize: 8,
                  fontWeight: FontWeight.w400,
                  color: AppColors.muted,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _showDetail(KolaTransaction tx) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _DetailSheet(
        transaction: tx,
        incoming: _isIncoming(tx),
        amount: _signedAmount(tx),
      ),
    );
  }
}

class _Summary extends StatelessWidget {
  const _Summary({
    required this.icon,
    required this.iconColor,
    required this.label,
    required this.value,
    required this.valueColor,
  });

  final IconData icon;
  final Color iconColor;
  final String label;
  final String value;
  final Color valueColor;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 17, color: iconColor),
        const SizedBox(height: 5),
        Text(
          label,
          style: AppTypography.badge.copyWith(
            fontSize: 8,
            color: const Color(0xFFADC6FF),
          ),
        ),
        const SizedBox(height: 2),
        Text(
          value,
          style: AppTypography.amount.copyWith(
            fontSize: 17,
            fontWeight: FontWeight.w900,
            color: valueColor,
          ),
        ),
      ],
    );
  }
}

class _HistoryRow extends StatelessWidget {
  const _HistoryRow({
    required this.transaction,
    required this.incoming,
    required this.amount,
    required this.withDivider,
    required this.onTap,
  });

  final KolaTransaction transaction;
  final bool incoming;
  final double amount;
  final bool withDivider;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final meta = metaFor(transaction.type);
    final note = transactionNote(transaction);
    final title = transaction.counterparty ?? note ?? meta.label;
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        constraints: const BoxConstraints(minHeight: 75),
        decoration: withDivider
            ? const BoxDecoration(
                border: Border(top: BorderSide(color: AppColors.border)),
              )
            : null,
        child: Row(
          children: [
            IconCircle(
              incoming ? KolaIcons.arrowDownOutline : KolaIcons.arrowUpOutline,
              background: incoming ? AppColors.greenPale : AppColors.pale2,
              color: incoming ? AppColors.greenDark : AppColors.primary,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.badge.copyWith(
                      fontSize: 12,
                      fontWeight: FontWeight.w800,
                      color: AppColors.ink,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    // Sous un titre qui est déjà le type, on donne le statut
                    // plutôt que de répéter le même mot.
                    title == meta.label
                        ? statusLabel(transaction)
                        : (note ?? meta.label),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.caption.copyWith(fontSize: 8),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    receiptDate(transaction.displayedAt),
                    style: AppTypography.caption.copyWith(fontSize: 8),
                  ),
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.xs),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  '${incoming ? '+' : '-'}${money(amount.abs())} F',
                  style: AppTypography.badge.copyWith(
                    fontSize: 11,
                    fontWeight: FontWeight.w900,
                    color: incoming ? AppColors.greenDark : AppColors.ink,
                  ),
                ),
                const SizedBox(height: 6),
                const Icon(
                  KolaIcons.chevronForward,
                  size: 15,
                  color: AppColors.muted,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailSheet extends StatelessWidget {
  const _DetailSheet({
    required this.transaction,
    required this.incoming,
    required this.amount,
  });

  final KolaTransaction transaction;
  final bool incoming;
  final double amount;

  @override
  Widget build(BuildContext context) {
    final meta = metaFor(transaction.type);

    return Container(
      padding: const EdgeInsets.fromLTRB(22, 22, 22, 34),
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppRadius.sheet),
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SheetHeader(title: 'Détail de la transaction'),
          const SizedBox(height: 18),
          Center(
            child: IconCircle(
              meta.icon,
              size: 29,
              diameter: 60,
              background: AppColors.pale2,
            ),
          ),
          const SizedBox(height: 10),
          Text(
            transaction.counterparty ??
                transactionNote(transaction) ??
                meta.label,
            textAlign: TextAlign.center,
            style: AppTypography.cardTitle.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 5),
          Text(
            '${incoming ? '+' : '-'}${money(amount.abs())} FCFA',
            textAlign: TextAlign.center,
            style: AppTypography.screenTitle.copyWith(
              fontSize: 25,
              color: incoming ? AppColors.greenDark : AppColors.ink,
            ),
          ),
          Container(
            margin: const EdgeInsets.symmetric(vertical: 17),
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              color: AppColors.pale,
              borderRadius: BorderRadius.circular(11),
            ),
            child: Column(
              children: [
                DetailRow(label: 'Type', value: meta.label),
                DetailRow(
                  label: 'Date',
                  value: receiptDate(transaction.displayedAt),
                ),
                if (transactionNote(transaction) case final note?)
                  DetailRow(label: 'Motif', value: note),
                DetailRow(label: 'Statut', value: statusLabel(transaction)),
                DetailRow(
                  label: 'Référence',
                  value: transaction.reference,
                  last: true,
                ),
              ],
            ),
          ),
          PrimaryButton(
            label: 'Fermer',
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
    );
  }
}
